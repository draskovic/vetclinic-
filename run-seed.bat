@echo off
set PGPASSWORD=admin
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d vetapp -f "C:\Users\ndraskovic\worksapceVetClinic\vetclinic\src\main\resources\seed-test-data.sql"
echo DONE
