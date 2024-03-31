export type LoginResponse = {
  message: string | null;
  success: true;
  token: string;
}

export type User = {
  username: string;
  token: string;
};
