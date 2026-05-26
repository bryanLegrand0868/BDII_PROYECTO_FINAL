import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';



@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles.html',
  styleUrl: './roles.css',
})
export class Roles implements OnInit {
  roles: any[] = [];
  loading = true;
  showModal = false;
  editingRole: any = null;
  formData = {
    roleName: '',
    description: '',
    parentRoleId: null as number | null,
    isOracleRole: 'N'
  };
  availableParents: any[] = [];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef  // 👈 Agrega esto
  ) {}

  ngOnInit() {
    console.log('Roles componente iniciado');
    this.loadRoles();
  }

  loadRoles() {
    console.log('Cargando roles...');
    this.loading = true;
    
    this.http.get('http://localhost:8080/api/roles').subscribe({
      next: (data: any) => {
        console.log('Datos recibidos:', data);
        this.roles = data;
        this.loading = false;
        this.cdr.detectChanges();  // 👈 Forzar actualización de la vista
        console.log('loading cambiado a false');
      },
      error: (error) => {
        console.error('Error:', error);
        this.loading = false;
        this.cdr.detectChanges();  // 👈 Forzar actualización
      }
    });
  }

  loadAvailableParents() {
    this.availableParents = this.roles.filter(r => !this.editingRole || r.ROLE_ID !== this.editingRole.ROLE_ID);
  }

  getParentName(parentId: number): string {
    if (!parentId) return '';
    const parent = this.roles.find(r => r.ROLE_ID === parentId);
    return parent ? parent.ROLE_NAME : '';
  }

  openModal(role?: any) {
    console.log('Abriendo modal, roles disponibles:', this.roles.length);
    if (role) {
      this.editingRole = role;
      this.formData = {
        roleName: role.ROLE_NAME || '',
        description: role.DESCRIPTION || '',
        parentRoleId: role.PARENT_ROLE_ID || null,
        isOracleRole: role.IS_ORACLE_ROLE || 'N'
      };
    } else {
      this.editingRole = null;
      this.formData = {
        roleName: '',
        description: '',
        parentRoleId: null,
        isOracleRole: 'N'
      };
    }
    this.loadAvailableParents();
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingRole = null;
  }

  saveRole() {
    const actorId = 1;
    
    if (this.editingRole) {
      this.http.put(`http://localhost:8080/api/roles/${this.editingRole.ROLE_ID}`, {
        roleName: this.formData.roleName,
        description: this.formData.description,
        parentRoleId: this.formData.parentRoleId,
        isOracleRole: this.formData.isOracleRole
      }).subscribe({
        next: () => {
          this.loadRoles();
          this.closeModal();
        },
        error: (error) => console.error('Error:', error)
      });
    } else {
      this.http.post(`http://localhost:8080/api/roles`, {
        roleName: this.formData.roleName,
        description: this.formData.description,
        parentRoleId: this.formData.parentRoleId,
        isOracleRole: this.formData.isOracleRole,
        createdBy: actorId
      }).subscribe({
        next: () => {
          this.loadRoles();
          this.closeModal();
        },
        error: (error) => console.error('Error:', error)
      });
    }
  }

  deleteRole(role: any) {
    const id = role.ROLE_ID;
    if (!id) return;
    
    if (confirm(`¿Eliminar el rol "${role.ROLE_NAME}"?`)) {
      const actorId = 1;
      this.http.delete(`http://localhost:8080/api/roles/${id}/${actorId}`).subscribe({
        next: () => this.loadRoles(),
        error: (error) => {
          console.error('Error:', error);
          alert('No se puede eliminar el rol porque tiene dependencias');
        }
      });
    }
  }
}