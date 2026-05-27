import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

interface TreeNode {
  ROLE_ID: number;
  ROLE_NAME: string;
  DESCRIPTION?: string;
  IS_ORACLE_ROLE?: string;
  PARENT_ROLE_ID?: number;
  children: TreeNode[];
  expanded?: boolean;
}

@Component({
  selector: 'app-hierarchy',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './hierarchy.html',
  styleUrl: './hierarchy.css',
})
export class Hierarchy implements OnInit {
  tree: TreeNode[] = [];
  loading = true;
  selected: TreeNode | null = null;
  selectedPerms: any[] = [];
  selectedGrantees: any[] = [];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.cdr.detectChanges();
    this.http.get('http://localhost:8080/api/roles/hierarchy').subscribe({
      next: (data: any) => {
        this.tree = this.buildTree(data || []);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  buildTree(rows: any[]): TreeNode[] {
    const map = new Map<number, TreeNode>();
    rows.forEach(r => map.set(r.ROLE_ID, { ...r, children: [], expanded: true }));
    const roots: TreeNode[] = [];
    map.forEach(node => {
      if (node.PARENT_ROLE_ID && map.has(node.PARENT_ROLE_ID)) {
        map.get(node.PARENT_ROLE_ID)!.children.push(node);
      } else {
        roots.push(node);
      }
    });
    return roots;
  }

  select(node: TreeNode) {
    this.selected = node;
    this.selectedPerms = [];
    this.selectedGrantees = [];
    this.cdr.detectChanges();

    this.http.get(`http://localhost:8080/api/permissions/role/${node.ROLE_ID}`)
      .subscribe((d: any) => { this.selectedPerms = d || []; this.cdr.detectChanges(); });
    this.http.get(`http://localhost:8080/api/roles/${node.ROLE_ID}/grantees`)
      .subscribe((d: any) => { this.selectedGrantees = d || []; this.cdr.detectChanges(); });
  }

  toggle(node: TreeNode) {
    node.expanded = !node.expanded;
    this.cdr.detectChanges();
  }
}
