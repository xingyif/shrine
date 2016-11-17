create table HUB_QUERY (
	NETWORK_QUERY_ID NUMBER(11) not null,
    DOMAIN VARCHAR2(256) not null,
    USERNAME VARCHAR2(256) not null,
    CREATE_DATE timestamp default current_timestamp,
    QUERY_DEFINITION CLOB not null,
	constraint hub_query_id_pk primary key(NETWORK_QUERY_ID)
);

create table HUB_QUERY_RESULT (
	ID NUMBER(11) not null,
    NETWORK_QUERY_ID NUMBER(11) not null,
    NODE_NAME VARCHAR2(255) not null,
    CREATE_DATE timestamp default current_timestamp,
    STATUS VARCHAR2(255) not null,
	constraint hub_query_result_id_pk primary key(ID)
);

-- handcrafted autoincrement from http://earlruby.org/2009/01/creating-auto-increment-columns-in-oracle/
create sequence HUB_QUERY_RESULT_id_Seq start with 1 increment by 1;
create or replace trigger HUB_QUERY_RESULT_id_Insert
before insert on "HUB_QUERY_RESULT"
for each row
declare
    max_id number;
    cur_seq number;
begin
    if :new.id is null then
        -- No ID passed, get one from the sequence
        select HUB_QUERY_RESULT_id_Seq.nextval into :new.id from dual;
    else
        -- ID was set via insert, so update the sequence
        select greatest(nvl(max(id),0), :new.id) into max_id from "HUB_QUERY_RESULT";
        select HUB_QUERY_RESULT_id_Seq.nextval into cur_seq from dual;
        while cur_seq < max_id
        loop
            select HUB_QUERY_RESULT_id_Seq.nextval into cur_seq from dual;
        end loop;
    end if;
end;
/
-- end autoincrement