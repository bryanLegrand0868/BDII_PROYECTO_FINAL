import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
    'ASSIGN_ROLE', 'REVOKE_ROLE', 'LOGIN', 'LOGIN_FAILED'
  ];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadAudit();
  }

loadAudit() {
  this.loading = true;
  console.log('Cargando auditoría...');
  
  this.http.get('http://localhost:8080/api/audit').subscribe({
    next: (data: any) => {
      console.log('Primer log:', data[0]);
      console.log('Todos los campos:', Object.keys(data[0])); 
      this.logs = data;
      this.loading = false;
    },
    error: (error) => {
      console.error('Error:', error);
      this.loading = false;
    }
  });
}

  applyFilters() {
    this.loading = true;
    let url = 'http://localhost:8080/api/audit/filter?';
    
    if (this.filterAction) url += `action=${this.filterAction}&`;
    if (this.filterUser) url += `user=${this.filterUser}&`;
    if (this.startDate) url += `startDate=${this.startDate}&`;
    if (this.endDate) url += `endDate=${this.endDate}&`;
    
    this.http.get(url).subscribe({
      next: (data: any) => {
        this.logs = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error:', error);
        this.loading = false;
      }
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
      'GRANT': 'bg-success',
      'REVOKE': 'bg-danger',
      'CREATE_ROLE': 'bg-primary',
      'DROP_ROLE': 'bg-dark',
      'CREATE_USER': 'bg-primary',
      'DROP_USER': 'bg-dark',
      'MODIFY_USER': 'bg-warning',
      'ASSIGN_ROLE': 'bg-info',
      'REVOKE_ROLE': 'bg-danger',
      'LOGIN': 'bg-success',
      'LOGIN_FAILED': 'bg-danger'
    };
    return badges[action] || 'bg-secondary';
  }

  getResultBadgeClass(result: string): string {
    return result === 'OK' ? 'bg-success' : 'bg-danger';
  }

  viewSQL(sql: string) {
    if (sql) {
      const ventana = window.open('', '_blank');
      if (ventana) {
        ventana.document.write(`
          <html>
            <head>
              <title>SQL Generado</title>
              <style>
                body { font-family: monospace; padding: 20px; background: #f5f5f5; }
                pre { background: #fff; padding: 15px; border-radius: 5px; border: 1px solid #ddd; white-space: pre-wrap; }
                button { margin-top: 10px; padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
                button:hover { background: #0056b3; }
              </style>
            </head>
            <body>
              <h3>SQL Generado</h3>
              <pre>${sql}</pre>
              <button onclick="window.close()">Cerrar</button>
            </body>
          </html>
        `);
      }
    } else {
      alert('No hay SQL asociado a esta acción');
    }
  }
}