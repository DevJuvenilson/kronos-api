-- Criar extensão para geração automática de UUIDs aleatórios no Postgres
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabela de Usuários
CREATE TABLE tb_user (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Tabela de Grupos
CREATE TABLE tb_group (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    invitation_code VARCHAR(6) UNIQUE NOT NULL
);

-- Tabela Associativa de Membros (Controle de Acesso por Contexto)
CREATE TABLE tb_group_member (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    group_role VARCHAR(30) NOT NULL,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_group FOREIGN KEY (group_id) REFERENCES tb_group(id) ON DELETE CASCADE
);

-- Tabela de Tarefas
CREATE TABLE tb_task (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    deadline TIMESTAMP NOT NULL,
    category VARCHAR(50),
    group_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    CONSTRAINT fk_task_group FOREIGN KEY (group_id) REFERENCES tb_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_user FOREIGN KEY (created_by_id) REFERENCES tb_user(id)
);