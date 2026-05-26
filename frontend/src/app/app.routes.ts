import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Dashboard } from './dashboard/dashboard';
import { Users } from './users/users';
import { AuthGuard } from './auth-guard';
import { Roles } from './roles/roles';
import { Audit } from './audit/audit';
import { Permissions } from './permissions/permissions';



export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'dashboard', component: Dashboard, canActivate: [AuthGuard] },
  { path: 'users', component: Users, canActivate: [AuthGuard] },
  { path: 'roles', component: Roles, canActivate: [AuthGuard] },
  { path: 'audit', component: Audit, canActivate: [AuthGuard] },
  { path: 'permissions', component: Permissions, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];