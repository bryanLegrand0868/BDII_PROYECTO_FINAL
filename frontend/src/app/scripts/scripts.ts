import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-scripts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './scripts.html',
  styleUrl: './scripts.css',
})
export class Scripts implements OnInit {
  scripts: any[] = [];
  filter = '';
  loading = true;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.cdr.detectChanges();
    this.http.get('http://localhost:8080/api/scripts').subscribe({
      next: (d: any) => { this.scripts = d || []; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  filtered() {
    if (!this.filter) return this.scripts;
    return this.scripts.filter(s => s.SCRIPT_TYPE === this.filter);
  }

  download() {
    const all = this.filtered().map(s =>
      `-- [${s.SCRIPT_ID}] ${s.SCRIPT_TYPE} — ${s.GENERATED_BY_USERNAME} — ${s.GENERATED_AT}\n` +
      `-- ${s.DESCRIPTION}\n${s.SCRIPT_CONTENT};\n`
    ).join('\n');
    const blob = new Blob([all], { type: 'text/sql' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'dcl-scripts.sql';
    a.click();
    URL.revokeObjectURL(url);
  }
}
