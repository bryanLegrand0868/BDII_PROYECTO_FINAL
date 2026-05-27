import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-db-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './db-users.html',
  styleUrl: './db-users.css',
})
export class DbUsers implements OnInit {
  users: any[] = [];
  loading = false;
  filter = '';
  lastSql = '';
  lastMessage = '';
  lastError = '';

  showCreate = false;
  newUser = { username: '', password: '', defaultTablespace: 'USERS', temporaryTablespace: 'TEMP' };

  showPwd = false;
  pwdUser = '';
  newPwd = '';

  showDetails = false;
  detailsUser: any = null;
  detailsRoles: any[] = [];
  detailsObj: any[] = [];
  detailsSys: any[] = [];

  showGrantRole = false;
  allRoles: any[] = [];
  grantRoleSelected: number | null = null;
  grantRoleAdmin = false;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.load(); this.loadRoles(); }

  load() {
    this.loading = true;
    this.cdr.detectChanges();
    this.http.get('http://localhost:8080/api/db-users').subscribe({
      next: (d: any) => { this.users = d || []; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  loadRoles() {
    this.http.get('http://localhost:8080/api/roles').subscribe({
      next: (d: any) => { this.allRoles = d || []; this.cdr.detectChanges(); }
    });
  }

  filtered() {
    if (!this.filter) return this.users;
    const f = this.filter.toUpperCase();
    return this.users.filter(u => u.USERNAME?.includes(f));
  }

  actorId(): number {
    const u = JSON.parse(localStorage.getItem('user') || '{}');
    return u.USER_ID || 1;
  }

  handleResult(res: any, ok: string) {
    this.lastSql = res.sql || '';
    this.lastMessage = res.message || ok;
    this.lastError = '';
    this.load();
    this.cdr.detectChanges();
  }

  handleError(err: any) {
    this.lastError = err.error?.error || err.message || 'Error desconocido';
    this.lastMessage = '';
    this.lastSql = '';
    this.cdr.detectChanges();
  }

  openCreate() {
    this.newUser = { username: '', password: '', defaultTablespace: 'USERS', temporaryTablespace: 'TEMP' };
    this.showCreate = true;
    this.cdr.detectChanges();
  }
  createUser() {
    const body = { ...this.newUser, actorId: this.actorId() };
    this.http.post('http://localhost:8080/api/db-users', body).subscribe({
      next: (r: any) => { this.handleResult(r, 'Usuario creado'); this.showCreate = false; this.cdr.detectChanges(); },
      error: (e) => this.handleError(e)
    });
  }

  lock(u: any) {
    if (!confirm(`¿Bloquear ${u.USERNAME}?`)) return;
    this.http.put(`http://localhost:8080/api/db-users/${u.USERNAME}/lock`, { actorId: this.actorId() })
      .subscribe({ next: (r: any) => this.handleResult(r, 'Bloqueado'), error: (e) => this.handleError(e) });
  }
  unlock(u: any) {
    this.http.put(`http://localhost:8080/api/db-users/${u.USERNAME}/unlock`, { actorId: this.actorId() })
      .subscribe({ next: (r: any) => this.handleResult(r, 'Desbloqueado'), error: (e) => this.handleError(e) });
  }

  openPwd(u: any) { this.pwdUser = u.USERNAME; this.newPwd = ''; this.showPwd = true; this.cdr.detectChanges(); }
  changePwd() {
    this.http.put(`http://localhost:8080/api/db-users/${this.pwdUser}/password`,
      { newPassword: this.newPwd, actorId: this.actorId() })
      .subscribe({
        next: (r: any) => { this.handleResult(r, 'Password cambiado'); this.showPwd = false; this.cdr.detectChanges(); },
        error: (e) => this.handleError(e)
      });
  }

  drop(u: any) {
    if (!confirm(`¿Eliminar usuario Oracle ${u.USERNAME}? Esto ejecuta DROP USER CASCADE.`)) return;
    this.http.delete(`http://localhost:8080/api/db-users/${u.USERNAME}/${this.actorId()}`)
      .subscribe({ next: (r: any) => this.handleResult(r, 'Eliminado'), error: (e) => this.handleError(e) });
  }

  details(u: any) {
    this.detailsUser = u;
    this.detailsRoles = []; this.detailsObj = []; this.detailsSys = [];
    this.showDetails = true;
    this.cdr.detectChanges();

    this.http.get(`http://localhost:8080/api/db-users/${u.USERNAME}/roles`)
      .subscribe((d: any) => { this.detailsRoles = d || []; this.cdr.detectChanges(); });
    this.http.get(`http://localhost:8080/api/db-users/${u.USERNAME}/object-privs`)
      .subscribe((d: any) => { this.detailsObj = d || []; this.cdr.detectChanges(); });
    this.http.get(`http://localhost:8080/api/db-users/${u.USERNAME}/system-privs`)
      .subscribe((d: any) => { this.detailsSys = d || []; this.cdr.detectChanges(); });
  }

  openGrantRole(u: any) {
    this.detailsUser = u;
    this.grantRoleSelected = null;
    this.grantRoleAdmin = false;
    this.showGrantRole = true;
    this.cdr.detectChanges();
  }
  grantRole() {
    if (!this.grantRoleSelected) return;
    const body = {
      roleId: this.grantRoleSelected,
      oracleUsername: this.detailsUser.USERNAME,
      adminOption: this.grantRoleAdmin ? 'S' : 'N',
      actorId: this.actorId()
    };
    this.http.post('http://localhost:8080/api/roles/grant-to-db-user', body).subscribe({
      next: (r: any) => { this.handleResult(r, 'Rol concedido'); this.showGrantRole = false; this.cdr.detectChanges(); },
      error: (e) => this.handleError(e)
    });
  }
}
