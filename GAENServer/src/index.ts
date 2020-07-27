import "reflect-metadata";
import {createConnection} from "typeorm";
import {Log} from "./entity/Log";
import {Config} from "./entity/Config"
import * as express from "express";
import * as ejs from "ejs"
import * as bodyParser from "body-parser"
import * as cors from "cors"
import * as asyncHandler from "express-async-handler"


const app = express();

app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');
app.engine('html', ejs.renderFile);
app.use(express.static('public'));

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cors());


console.log("Connecting to MySQL DB...");
createConnection().then(async connection => {
    console.log("Connected to MySQL DB.");

    console.log("Initializing config...");
    
    const defaultConfig = new Config();
    defaultConfig.version = 0;
    defaultConfig.SCAN_PERIOD = '300000';
    defaultConfig.SCAN_DURATION = '8000';
    defaultConfig.SERVICE_UUID = 'aa';
    defaultConfig.advertiseMode = 0;
    defaultConfig.advertiseTxPower = 3;
    defaultConfig.scanMode = 0;

    await connection.manager.getRepository(Config)
        .createQueryBuilder()
        .insert()
        .orIgnore()
        .into(Config)
        .values(defaultConfig)
        .execute();
    
    console.log("Initialized config.");

    app.listen(80, () => console.log("Express server has started on port 80"))

    app.get('/config', asyncHandler(async (req, res, next) => {
        let config: Config;
        try {
            config = await connection.manager.getRepository(Config).findOne();
        } catch {
            res.status(500).send("Failed to fetch config.");
            return;
        }
        res.status(200).send(config);
    }));

}).catch(error => console.log(error));
