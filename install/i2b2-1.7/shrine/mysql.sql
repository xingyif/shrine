-- Create the SHRINE database
drop database if exists SHRINE_DB_NAME;
create database SHRINE_DB_NAME; 

-- Create a SQL user for query history
grant all privileges on SHRINE_DB_NAME.* to SHRINE_MYSQL_USER@SHRINE_MYSQL_HOST identified by 'SHRINE_MYSQL_PASSWORD';

-- Create the steward database
drop database if exists SHRINE_STEWARD_DB_NAME;
create database SHRINE_STEWARD_DB_NAME; 

-- Create a SQL user for query history
grant all privileges on SHRINE_STEWARD_DB_NAME.* to SHRINE_STEWARD_MYSQL_USER@SHRINE_STEWARD_MYSQL_HOST identified by 'SHRINE_STEWARD_MYSQL_PASSWORD';
