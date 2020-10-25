DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
sudo docker run --name gaen-mysql-server -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=GAENtest \
    -v ${DIR}/../../GAENServer/MySQL_Commands.sql:/docker-entrypoint-initdb.d/MySQL_Commands.sql \
    -d mysql/mysql-server:latest \
    --default-authentication-plugin=mysql_native_password

