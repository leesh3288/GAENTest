import "reflect-metadata";
import {createConnection} from "typeorm";
import {Log} from "./entity/Log";
import {Config} from "./entity/Config"


console.log("Connecting to MySQL DB...");
createConnection().then(async connection => {
    console.log("Connected to MySQL DB.");

    console.log("Initializing config...");
    
    const config = new Config();
    config.version = 0;
    config.SCAN_PERIOD = 300000;
    config.SCAN_DURATION = 8000;
    config.SERVICE_UUID = 'aa';
    config.advertiseMode = 0;
    config.advertiseTxPower = 3;
    config.scanMode = 0;

    await connection.manager.getRepository(Config)
        .createQueryBuilder()
        .insert()
        .orIgnore()
        .into(Config)
        .values(config)
        .execute();
    
    console.log("Initialized config.");

    /// DEBUG ///
    console.log(await connection.manager.getRepository(Config).find());

}).catch(error => console.log(error));
