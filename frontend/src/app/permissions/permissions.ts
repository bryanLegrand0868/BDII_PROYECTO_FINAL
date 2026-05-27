import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-permissions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './permissions.html',
  styleUrl: './permissions.css',
})
export class Permissions implements OnInit {
  roles: any[] = [];
  permissions: any[] = [];
  selectedRole: any = null;
  loading = false;
  
  availablePrivileges = ['SELECT', 'INSERT', 'UPDATE', 'DELETE', 'EXECUTE', 'ALL'];
  
  showGrantModal = false;
  grantData = {
    roleId: null as number | null,
    permissionId: null as number | null,
    privilege: 'SELECT',
    schema: 'HR',
    objectName: 'EMPLOYEES',
    objectType: 'TABLE',
    grantOption: false
  };

  allPermissions: any[] = [];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadRoles();
    this.loadAllPermissions();
  }

  loadRoles() {
    this.http.get('http://localhost:8080/api/roles').subscribe({
      next: (data: any) => {
        this.roles = [...data];
        this.cdr.detectChanges();
        console.log('Roles cargados:', this.roles.length);
      },
      error: (error) => console.error('Error cargando roles:', error)
    });
  }

  loadAllPermissions() {
    this.http.get('http://localhost:8080/api/permissions').subscribe({
      next: (data: any) => {
        this.allPermissions = [...data];
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Error:', error)
    });
  }

  selectRole(role: any) {
    this.selectedRole = { ...role };
    this.loadRolePermissions(role.ROLE_ID);
  }

  loadRolePermissions(roleId: number) {
    this.loading = true;
    this.permissions = [];
    this.cdr.detectChanges();

    this.http.get(`http://localhost:8080/api/permissions/role/${roleId}`).subscribe({
      next: (data: any) => {
        this.permissions = Array.isArray(data) ? [...data] : [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error cargando permisos:', error);
        this.permissions = [];
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openGrantModal() {
    if (!this.selectedRole) { alert('Primero selecciona un rol'); return; }
    this.grantData = {
      roleId: this.selectedRole.ROLE_ID,
      permissionId: null,
      privilege: 'SELECT',
      schema: 'HR',
      objectName: 'EMPLOYEES',
      objectType: 'TABLE',
      grantOption: false
    };
    this.showGrantModal = true;
    this.cdr.detectChanges();
  }

  closeGrantModal() {
    this.showGrantModal = false;
    this.cdr.detectChanges();
  }

  savePermission() {
    const actorId = 1;

    // Buscar si el permiso ya existe en allPermissions
    const existing = this.allPermissions.find(p =>
      p.OBJECT_NAME === this.grantData.objectName &&
      p.PRIVILEGE_TYPE === this.grantData.privilege &&
      p.SCHEMA_NAME === this.grantData.schema &&
      p.OBJECT_TYPE === this.grantData.objectType
    );

    if (existing) {
      // Ya existe, asignar directo al rol
      this.assignPermissionToRole(existing.PERMISSION_ID, actorId);
    } else {
      // No existe, crear primero
      const permissionData = {
        objectName: this.grantData.objectName,
        objectType: this.grantData.objectType,
        privilegeType: this.grantData.privilege,
        schemaName: this.grantData.schema,
        description: `${this.grantData.privilege} on ${this.grantData.objectName}`
      };

      this.http.post('http://localhost:8080/api/permissions', permissionData).subscribe({
        next: (perm: any) => {
          this.allPermissions = [...this.allPermissions, perm]; // actualizar lista local
          this.assignPermissionToRole(perm.permissionId, actorId);
        },
        error: (error) => console.error('Error al crear permiso:', error)
      });
    }
  }

  assignPermissionToRole(permissionId: number, actorId: number) {
    const grantData = {
      roleId: this.grantData.roleId,
      permissionId: permissionId,
      grantOption: this.grantData.grantOption ? 'S' : 'N',
      grantedBy: actorId
    };

    this.http.post('http://localhost:8080/api/permissions/grant-role', grantData).subscribe({
      next: () => {
        this.loadRolePermissions(this.grantData.roleId!);
        this.loadAllPermissions(); // refrescar lista de permisos disponibles
        this.closeGrantModal();
        alert('Permiso asignado correctamente ✅');
      },
      error: (error) => {
        console.error('Error al asignar:', error);
        alert('❌ Error: ese permiso ya está asignado a este rol');
      }
    });
  }

  revokePermission(permission: any) {
    if (confirm(`¿Revocar permiso ${permission.PRIVILEGE_TYPE} sobre ${permission.OBJECT_NAME}?`)) {
      const actorId = 1;
      this.http.delete(`http://localhost:8080/api/permissions/grant-role/${this.selectedRole.ROLE_ID}/${permission.PERMISSION_ID}/${actorId}`).subscribe({
        next: () => {
          this.loadRolePermissions(this.selectedRole.ROLE_ID);
          alert('Permiso revocado ✅');
        },
        error: (error) => console.error('Error:', error)
      });
    }
  }

  getPrivilegeBadgeClass(privilege: string): string {
    const badges: Record<string, string> = {
      'SELECT': 'bg-success',
      'INSERT': 'bg-primary',
      'UPDATE': 'bg-warning',
      'DELETE': 'bg-danger',
      'EXECUTE': 'bg-info',
      'ALL': 'bg-dark'
    };
    return badges[privilege] || 'bg-secondary';
  }
}