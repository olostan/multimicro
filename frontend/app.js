var express = require('express');    //Express Web Server 
var busboy = require('connect-busboy'); //middleware for form/file upload
var path = require('path');     //used for file path
var fs = require('fs');       //File System - for file manipulation
var app = express();
var streamBuffers = require("stream-buffers");

var engine = require('hbs');

app.use(busboy());
app.use(express.static(path.join(__dirname, 'public')));


app.set('view engine', 'hbs');

var grpc = require('grpc');

var dispatcher_proto = grpc.load('dispatcher.proto').dispatcher;

var dispatcherUrl = process.env['DISPATCHER'] || 'localhost';
console.log("Dispatcher "+dispatcherUrl);
var client = new dispatcher_proto.Dispatcher(dispatcherUrl+':50051',
    grpc.Credentials.createInsecure());


app.route('/')
    ///.get((req,res) => res.render('index'))
    .get(function(req,res) { res.locals = {aaa:333}; res.render('index');})
    .post(function (req, res) {

        req.pipe(req.busboy);
        req.busboy.on('file', function (fieldname, file, filename) {
            if (!filename) {
                res.status(400).send("No image uploader");
                return;
            }
            console.log("Uploading: " + filename);

            var writer = new streamBuffers.WritableStreamBuffer();
            file.pipe(writer);
            writer.on('finish', function () {
                console.log("Upload Finished of " + filename);
                client.dispatch({image:writer.getContents()}, function(err, response) {
                   if (err) {
                       res.status(500).send(err);
                       console.error(err);
                       return;
                   }
                    res.render('index.hbs',{name:filename, image:response.image.toString('base64')})
                });
            //    console.log(writer.getContentsAsString("base64"));              
                //res.redirect('back');           //where to go next
                //res.render('result.hbs',{name:filename});
            });
        });
    });

var server = app.listen(process.env.PORT || 8080, function() {
    console.log('Listening on port %d', server.address().port);
});
