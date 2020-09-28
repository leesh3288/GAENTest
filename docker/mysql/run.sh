sudo docker run --name gaen-mysql-server -p 3306:3306 \
    -v ${PWD}/../../GAENServer/MySQL_Commands.sql:/docker-entrypoint-initdb.d/MySQL_Commands.sql \
    -d mysql/mysql-server:latest \
    --default-authentication-plugin=mysql_native_password
