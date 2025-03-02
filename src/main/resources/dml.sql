select * from pg_extension; -- vector extension should be listed

select * from vector_store limit 3;

delete from vector_store where vector_store.content is not null;
commit ;