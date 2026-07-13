-- =====================================================================
-- V11 — Usuário administrador inicial
-- Credenciais padrão: admin@print3d.com / admin123 (BCrypt custo 10).
-- ATENÇÃO: trocar a senha no primeiro acesso em produção.
-- =====================================================================

INSERT INTO usuarios (nome, email, senha, role, ativo)
VALUES (
    'Administrador',
    'admin@print3d.com',
    '$2y$10$Nk2GLMICOXaU4cVt16vic.btF02THrIJnXlA/w8RA8Wc6XQYXXxAa',
    'ADMINISTRADOR',
    TRUE
);
