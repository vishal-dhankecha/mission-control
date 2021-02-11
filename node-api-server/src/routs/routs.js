'use strict';
module.exports = function(app) {
  var statusController = require('../controllers/statusController');
  var testController = require('../controllers/testController');

  // todoList Routes
    app.route('/server-status')
        .get(statusController.getStatus)
        .post(statusController.postStatus);
};