import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './users.html',
  styleUrl: './users.css',
})
export class Users implements OnInit {
  users: any[] = [];
  loading = true;
  showModal = false;
  editingUser: any = null;
  formData = {
    username: '',
    email: '',
    fullName: '',
    password: '',
    appRole: 'VIEWER',
    status: 'ACTIVO',
    oracleUsername: ''
  };

  // Variables para asignar roles
  showRolesModal = false;
  selectedUser: any = null;
  allRoles: any[] = [];
  userRoles: any[] = [];
  selectedRoles: number[] = [];
  loadingRoles = false;

  // Variables para permisos del usuario logueado
  userPermissions: string[] = [];
  currentUserId: number = 0;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadCurrentUser();
  }

  // ========== CARGAR USUARIO ACTUAL Y SUS PERMISOS ==========
  
  loadCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const currentUser = JSON.parse(userStr);
      this.currentUserId = currentUser.USER_ID;
      this.loadUserPermissions();
    }
    this.loadUsers();
  }

  loadUserPermissions() {
    this.http.get('http://localhost:8080/api/users/current/permissions').subscribe({
      next: (data: any) => {
        this.userPermissions = data.map((p: any) => `${p.OBJECT_NAME}_${p.PRIVILEGE_TYPE}`);
        console.log('Permisos del usuario logueado:', this.userPermissions);
      },
      error: (error) => {
        console.error('Error cargando permisos:', error);
      }
    });
  }

  hasPermission(objectName: string, privilege: string): boolean {
    // SUPERADMIN puede todo, sin importar el catálogo granular.
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const u = JSON.parse(userStr);
        if (u.APP_ROLE === 'SUPERADMIN') return true;
      } catch {}
    }
    return this.userPermissions.includes(`${objectName}_${privilege}`);
  }

  // ========== MÉTODOS PARA CARGAR ROLES DE CADA USUARIO ==========
  
  loadUserRoles(user: any) {
    this.http.get(`http://localhost:8080/api/users/${user.USER_ID}/roles`).subscribe({
      next: (roles: any) => {
        user.userRoles = roles;
        console.log(`Roles de ${user.USERNAME}:`, roles);
      },
      error: (error) => {
        console.error(`Error al cargar roles de ${user.USERNAME}:`, error);
        user.userRoles = [];
      }
    });
  }

  loadUsers() {
    this.loading = true;
    this.cdr.detectChanges();
    this.http.get('http://localhost:8080/api/users').subscribe({
      next: (data: any) => {
        this.users = data || [];
        this.users.forEach((user: any) => this.loadUserRoles(user));
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openModal(user?: any) {
    // Si no tiene permiso de INSERT y no está editando, no puede abrir modal de creación
    if (!user && !this.hasPermission('APP_USERS', 'INSERT')) {
      alert('No tienes permiso para crear usuarios');
      return;
    }
    
    // Si está editando y no tiene permiso de UPDATE, no puede editar
    if (user && !this.hasPermission('APP_USERS', 'UPDATE')) {
      alert('No tienes permiso para editar usuarios');
      return;
    }
    
    if (user) {
      this.editingUser = user;
      this.formData = {
        username: user.USERNAME || '',
        email: user.EMAIL || '',
        fullName: user.FULL_NAME || '',
        password: '',
        appRole: user.APP_ROLE || 'VIEWER',
        status: user.STATUS || 'ACTIVO',
        oracleUsername: user.ORACLE_USERNAME || ''
      };
    } else {
      this.editingUser = null;
      this.formData = {
        username: '',
        email: '',
        fullName: '',
        password: '',
        appRole: 'VIEWER',
        status: 'ACTIVO',
        oracleUsername: ''
      };
    }
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingUser = null;
  }

  saveUser() {
    const actorId = this.currentUserId || 21;
    
    if (this.editingUser) {
      // Verificar permiso UPDATE
      if (!this.hasPermission('APP_USERS', 'UPDATE')) {
        alert('No tienes permiso para actualizar usuarios');
        return;
      }
      
      const userId = this.editingUser.USER_ID;
      const dataToUpdate = {
        username: this.formData.username,
        email: this.formData.email,
        fullName: this.formData.fullName,
        appRole: this.formData.appRole,
        status: this.formData.status,
        oracleUsername: this.formData.oracleUsername || null,
        createdBy: actorId
      };
      
      console.log('Actualizando usuario:', userId, dataToUpdate);
      
      this.http.put(`http://localhost:8080/api/users/${userId}`, dataToUpdate).subscribe({
        next: (response) => {
          console.log('Usuario actualizado:', response);
          this.loadUsers();
          this.closeModal();
        },
        error: (error) => {
          console.error('Error al actualizar:', error);
          alert('Error al actualizar: ' + (error.error?.error || error.message));
        }
      });
    } else {
      // Verificar permiso INSERT
      if (!this.hasPermission('APP_USERS', 'INSERT')) {
        alert('No tienes permiso para crear usuarios');
        return;
      }
      
      const dataToSend = {
        username: this.formData.username,
        email: this.formData.email,
        fullName: this.formData.fullName,
        password: this.formData.password,
        appRole: this.formData.appRole,
        status: this.formData.status,
        createdBy: actorId,
        oracleUsername: this.formData.oracleUsername || null,
        oraclePassword: null
      };
      
      console.log('Creando usuario:', dataToSend);
      
      this.http.post('http://localhost:8080/api/users', dataToSend).subscribe({
        next: (response) => {
          console.log('Usuario creado:', response);
          this.loadUsers();
          this.closeModal();
        },
        error: (error) => {
          console.error('Error al crear:', error);
          alert('Error al crear: ' + (error.error?.error || error.message));
        }
      });
    }
  }

  deleteUser(user: any) {
    // Verificar permiso DELETE
    if (!this.hasPermission('APP_USERS', 'DELETE')) {
      alert('No tienes permiso para eliminar usuarios');
      return;
    }
    
    const id = user.USER_ID;
    if (!id) {
      console.error('ID no válido', user);
      return;
    }
    
    if (confirm('¿Eliminar este usuario?')) {
      const actorId = this.currentUserId || 1;
      this.http.delete(`http://localhost:8080/api/users/${id}/${actorId}`).subscribe({
        next: () => this.loadUsers(),
        error: (error) => console.error('Error:', error)
      });
    }
  }

  // ========== MÉTODOS PARA ASIGNAR ROLES ==========
  
  assignRoles(user: any) {
    // Verificar si tiene permiso para asignar roles (INSERT sobre USER_ROLES)
    if (!this.hasPermission('USER_ROLES', 'INSERT')) {
      alert('No tienes permiso para asignar roles');
      return;
    }
    
    console.log('=== ABRIENDO MODAL ===');
    console.log('Usuario:', user);
    this.selectedUser = user;
    this.showRolesModal = true;
    console.log('showRolesModal =', this.showRolesModal);
    this.loadRolesForUser();
  }

  loadRolesForUser() {
    this.loadingRoles = true;
    console.log('=== Cargando roles ===');
    
    this.http.get('http://localhost:8080/api/roles').subscribe({
      next: (roles: any) => {
        console.log('Roles disponibles:', roles);
        this.allRoles = roles;
        
        // Intentar cargar roles del usuario
        this.http.get(`http://localhost:8080/api/users/${this.selectedUser.USER_ID}/roles`).subscribe({
          next: (userRolesData: any) => {
            console.log('Roles actuales del usuario:', userRolesData);
            this.userRoles = userRolesData;
            this.selectedRoles = userRolesData.map((r: any) => r.ROLE_ID);
            this.loadingRoles = false;
          },
          error: (error) => {
            console.error('Error al cargar roles del usuario:', error);
            this.selectedRoles = [];
            this.loadingRoles = false;
          }
        });
      },
      error: (error) => {
        console.error('Error al cargar todos los roles:', error);
        this.loadingRoles = false;
      }
    });
  }

  isRoleAssigned(role: any): boolean {
    return this.selectedRoles.includes(role.ROLE_ID);
  }

  toggleRole(role: any) {
    if (this.isRoleAssigned(role)) {
      this.selectedRoles = this.selectedRoles.filter(id => id !== role.ROLE_ID);
    } else {
      this.selectedRoles.push(role.ROLE_ID);
    }
  }

  saveRolesAssignment() {
    const actorId = this.currentUserId || 21;
    
    const currentRoleIds = this.userRoles.map(r => r.ROLE_ID);
    const toAdd = this.selectedRoles.filter(id => !currentRoleIds.includes(id));
    const toRemove = currentRoleIds.filter(id => !this.selectedRoles.includes(id));
    
    console.log('Roles a agregar:', toAdd);
    console.log('Roles a quitar:', toRemove);
    
    for (const roleId of toAdd) {
      this.http.post('http://localhost:8080/api/roles/assign', {
        userId: this.selectedUser.USER_ID,
        roleId: roleId,
        assignedBy: actorId
      }).subscribe({
        next: () => console.log(`Rol ${roleId} asignado`),
        error: (error) => console.error('Error al asignar:', error)
      });
    }
    
    for (const roleId of toRemove) {
      this.http.delete(`http://localhost:8080/api/roles/assign/${this.selectedUser.USER_ID}/${roleId}/${actorId}`).subscribe({
        next: () => console.log(`Rol ${roleId} revocado`),
        error: (error) => console.error('Error al revocar:', error)
      });
    }
    
    setTimeout(() => {
      this.closeRolesModal();
      this.loadUsers();
    }, 500);
  }

  closeRolesModal() {
    this.showRolesModal = false;
    this.selectedUser = null;
    this.allRoles = [];
    this.userRoles = [];
    this.selectedRoles = [];
  }
}