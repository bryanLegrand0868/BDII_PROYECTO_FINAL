import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-system',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './system.html',
  styleUrl: './system.css',
})
export class System implements OnInit {
  loading = true;
  info: any = null;
  dbUsers: any[] = [];
  error = '';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.refresh(); }

  refresh() {
    this.loading = true;
    this.error = '';
    this.info = null;
    this.dbUsers = [];
    this.cdr.detectChanges();

    this.http.get('http://localhost:8080/api/system/info').subscribe({
      next: (data: any) => {
        this.info = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err.error?.error || 'No se pudo conectar al DBMS';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });

    this.http.get('http://localhost:8080/api/system/db-users').subscribe({
      next: (data: any) => {
        this.dbUsers = data || [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.dbUsers = [];
        this.cdr.detectChanges();
      }
    });
  }
}
