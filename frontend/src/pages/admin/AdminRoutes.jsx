import { Navigate, Route, Routes } from 'react-router-dom';
import AdminLayout from './AdminLayout';
import AdminDashboardPage from './AdminDashboardPage';
import AdminUsersPage from './AdminUsersPage';
import AdminSystemSettingsPage from './AdminSystemSettingsPage';
import AdminAuditLogsPage from './AdminAuditLogsPage';

export default function AdminRoutes(){return <Routes><Route element={<AdminLayout/>}><Route index element={<AdminDashboardPage/>}/><Route path="users" element={<AdminUsersPage/>}/><Route path="settings" element={<AdminSystemSettingsPage/>}/><Route path="audit-logs" element={<AdminAuditLogsPage/>}/><Route path="*" element={<Navigate replace to="/admin"/>}/></Route></Routes>;}
