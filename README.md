# README

This project consists of two parts: a server part and an android app.

The server stores a purchase list, which can be read and modified by
the app, including offline functionality (last result will be stored
and offline changes will be executed later when possible).

The android app uses
[cert4android](https://gitlab.com/bitfireAT/cert4android.git),
released under the GPL v3,
in order to be able to access to servers using https with a self-signed
certificate.

The server side consists of an sql script to create the required table
and permissions on the mysql-server of your choice (make sure to change
the username and password to your setup before executing), and a few
php-scripts that are used to access the database from the app. Make also
sure to create a db_config.php (you can use example_php_config.php as
template) to configure the database access for the php-scripts.