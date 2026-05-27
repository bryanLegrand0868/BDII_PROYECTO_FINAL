import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './audit.html',
  styleUrl: './audit.css',
})
export class Audit implements OnInit {
  logs: any[] = [];
  loading = true;
  filterAction = '';
  filterUser = '';
  startDate = '';
  endDate = '';

  actions = [
    'GRANT', 'REVOKE', 'CREATE_ROLE', 'DROP_ROLE',
    'CREATE_USER', 'DROP_USER', 'MODIFY_USER',
    'LOCK_USER', 'UNLOCK_USER', 'CHANGE_PASSWORD',
    'ASSIGN_ROLE', 'REVOKE_ROLE',
    'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'TEST_ACCESS'
  ];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadAudit(); }

  loadAudit() {
    this.loading = true;
    this.cdr.detectChanges();
    this.http.get('http://localhost:8080/api/audit').subscribe({
      next: (data: any) => { this.logs = data || []; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  applyFilters() {
    this.loading = true;
    this.cdr.detectChanges();
    let url = 'http://localhost:8080/api/audit/filter?';
    if (this.filterAction) url += `action=${this.filterAction}&`;
    if (this.filterUser) url += `user=${this.filterUser}&`;
    if (this.startDate) url += `startDate=${this.startDate}&`;
    if (this.endDate) url += `endDate=${this.endDate}&`;

    this.http.get(url).subscribe({
      next: (data: any) => { this.logs = data || []; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  clearFilters() {
    this.filterAction = '';
    this.filterUser = '';
    this.startDate = '';
    this.endDate = '';
    this.loadAudit();
  }

  getActionBadgeClass(action: string): string {
    const badges: Record<string, string> = {
      'GRANT': 'bg-success', 'REVOKE': 'bg-danger',
      'CREATE_ROLE': 'bg-primary', 'DROP_ROLE': 'bg-dark',
      'CREATE_USER': 'bg-primary', 'DROP_USER': 'bg-dark',
      'MODIFY_USER': 'bg-warning', 'LOCK_USER': 'bg-warning',
      'UNLOCK_USER': 'bg-info', 'CHANGE_PASSWORD': 'bg-warning',
      'ASSIGN_ROLE': 'bg-info', 'REVOKE_ROLE': 'bg-danger',
      'LOGIN': 'bg-success', 'LOGOUT': 'bg-secondary',
      'LOGIN_FAILED': 'bg-danger', 'TEST_ACCESS': 'bg-info'
    };
    return badges[action] || 'bg-secondary';
  }

  viewSQL(sql: string) {
    if (sql) {
      const ventana = window.open('', '_blank');
      if (ventana) {
        ventana.document.write(`
          <html><head><title>SQL Generado</title>
            <style>body { font-family: monospace; padding: 20px; background: #f5f5f5; }
            pre { background: #fff; padding: 15px; border-radius: 5px; border: 1px solid #ddd; white-space: pre-wrap; }
            button { margin-top: 10px; padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }</style>
            </head><body><h3>SQL Generado</h3><pre>${sql}</pre>
            <button onclick="window.close()">Cerrar</button></body></html>`);
      }
    } else {
      alert('No hay SQL asociado a esta acción');
    }
  }
}
