import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-test-access',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './test-access.html',
  styleUrl: './test-access.css',
})
export class TestAccess implements OnInit {
  username = '';
  password = '';
  sql = 'SELECT COUNT(*) AS TOTAL FROM HR.EMPLOYEES';
  loading = false;
  result: any = null;
  dbUsers: any[] = [];

  presets = [
    { label: 'COUNT HR.EMPLOYEES',    sql: 'SELECT COUNT(*) AS TOTAL FROM HR.EMPLOYEES' },
    { label: 'SELECT HR.DEPARTMENTS', sql: 'SELECT * FROM HR.DEPARTMENTS' },
    { label: 'SELECT HR.JOBS',        sql: 'SELECT * FROM HR.JOBS' },
    { label: 'Ver SESSION_USER',      sql: "SELECT SYS_CONTEXT('USERENV','SESSION_USER') AS WHO FROM DUAL" },
    { label: 'Mis tablas',            sql: 'SELECT TABLE_NAME FROM USER_TABLES' },
  ];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.http.get('http://localhost:8080/api/system/db-users').subscribe({
      next: (d: any) => { this.dbUsers = d || []; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  actorId(): number {
    const u = JSON.parse(localStorage.getItem('user') || '{}');
    return u.USER_ID || 1;
  }

  test() {
    this.loading = true;
    this.result = null;
    this.cdr.detectChanges();
    this.http.post('http://localhost:8080/api/system/test-access', {
      username: this.username,
      password: this.password,
      sql: this.sql,
      actorId: this.actorId()
    }).subscribe({
      next: (r: any) => { this.result = r; this.loading = false; this.cdr.detectChanges(); },
      error: (e) => {
        this.result = { ok: false, error: e.error?.error || e.message, rows: [] };
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  columns(): string[] {
    if (!this.result?.rows?.length) return [];
    return Object.keys(this.result.rows[0]);
  }
}
