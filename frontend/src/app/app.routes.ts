import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Dashboard } from './dashboard/dashboard';
import { Users } from './users/users';
import { AuthGuard } from './auth-guard';
import { Roles } from './roles/roles';
import { Audit } from './audit/audit';
import { Permissions } from './permissions/permissions';
import { System } from './system/system';
import { DbUsers } from './db-users/db-users';
import { TestAccess } from './test-access/test-access';
import { Hierarchy } from './hierarchy/hierarchy';
import { Scripts } from './scripts/scripts';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'dashboard',    component: Dashboard,    canActivate: [AuthGuard] },
  { path: 'system',       component: System,       canActivate: [AuthGuard] },
  { path: 'users',        component: Users,        canActivate: [AuthGuard] },
  { path: 'db-users',     component: DbUsers,      canActivate: [AuthGuard] },
  { path: 'roles',        component: Roles,        canActivate: [AuthGuard] },
  { path: 'hierarchy',    component: Hierarchy,    canActivate: [AuthGuard] },
  { path: 'permissions',  component: Permissions,  canActivate: [AuthGuard] },
  { path: 'test-access',  component: TestAccess,   canActivate: [AuthGuard] },
  { path: 'audit',        component: Audit,        canActivate: [AuthGuard] },
  { path: 'scripts',      component: Scripts,      canActivate: [AuthGuard] },
  { path: '',             redirectTo: '/login',    pathMatch: 'full' }
];
