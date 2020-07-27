sudo docker run --name gaen-mysql-server -p 3306:3306 \
    -e MYSQL_USER=GAEN \
    -e MYSQL_PASSWORD=GAENtest \
    -e MYSQL_DATABASE=gaen_db \
    -d mysql/mysql-server:latest \
    --default-authentication-plugin=mysql_native_password
