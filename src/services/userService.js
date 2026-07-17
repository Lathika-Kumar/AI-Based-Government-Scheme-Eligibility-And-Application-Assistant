// src/services/userService.js

/** Mock data for users */
const mockUsers = [
  {
    id: "USER-1001",
    name: "Sanjay Kumar",
    role: "Super Admin",
    department: "Government Scheme Evaluation Board",
    email: "sanjay.kumar@schemebridge.in",
    avatar: "https://i.pravatar.cc/150?img=1",
    status: "active",
    lastLogin: "2026-07-01T09:15:00Z",
  },
  {
    id: "USER-1002",
    name: "Anita Sharma",
    role: "Verification Officer",
    department: "Ministry of Finance",
    email: "anita.sharma@schemebridge.in",
    avatar: "https://i.pravatar.cc/150?img=2",
    status: "active",
    lastLogin: "2026-07-01T08:45:00Z",
  },
  // Add more mock users as needed
];

export const fetchUsers = async () => {
  // Simulate async fetch
  return Promise.resolve([...mockUsers]);
};

export const updateUser = async (updatedUser) => {
  const idx = mockUsers.findIndex((u) => u.id === updatedUser.id);
  if (idx !== -1) {
    mockUsers[idx] = { ...mockUsers[idx], ...updatedUser };
  }
  return Promise.resolve({ ...mockUsers[idx] });
};

export const resetPassword = async (userId) => {
  // Mock implementation – just resolves
  return Promise.resolve({ success: true, userId });
};

export const assignRole = async (userId, newRole) => {
  const user = mockUsers.find((u) => u.id === userId);
  if (user) {
    user.role = newRole;
  }
  return Promise.resolve({ ...user });
};

const userService = { fetchUsers, updateUser, resetPassword, assignRole };
export default userService;
