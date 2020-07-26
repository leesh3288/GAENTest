const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const mysql = require('mysql');

const app = express();

app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');
app.engine('html', require('ejs').renderFile);
app.use(express.static('public'));

// parse application/json
app.use(bodyParser.json());
app.use(cors());

/************************
 *                      *
 *    DB Connection     *
 *                      *
 ************************/

// Create MySQL Connection Object
var connection = mysql.createConnection({
    host: 'localhost',
    port: 3306,
    user: 'GAEN',
    password: 'GAENtest',
    database: 'gaen_db'
});

// MySQL Connect
connection.connect(function (err) {
if (err) {
    console.error('mysql connection error');     
    console.error(err);
    throw err;
} 
else {
    console.log("mysql successfully connected");
}
});

/************************
 *                      *
 *   Request Handling   *
 *                      *
 ************************/

app.listen(80, function(){
  console.log("Express server has started on port 80")
})

// app.get('/test', function(req,res) {
//     console.log("GET test called.");
//     res.status(200);
//     res.send("This is the result for the test GET method.\nDo the two lines print properly?");
// })

app.get('/config', function (req, res) {
    connection.query(
        'SELECT * FROM config', user, function (err, row) {
        if (err) { throw err }
        else {
            console.log(row[0]);
        }
      });
    res.status(200);
    res.send(row);
  });

/************************
 *                      *
 *   Helper Functions   *
 *                      *
 ************************/

