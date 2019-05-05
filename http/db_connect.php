<?php

define("ROLE_ADMIN", "admin");
define("ROLE_USER", "user");
define("ROLE_OTHER", "other");

class DB_CONNECT {
    var $con;
    var $user_override;
    var $user_role;

    function __construct() {
        $this->connect();
    }
    function __destruct() {
        $this->close();
    }

    function getConnection() {
        return $con;
    }

    function connect() {
        require_once __DIR__ . '/db_config.php';
        $this->con = new mysqli(DB_SERVER, DB_USER, DB_PASSWORD, DB_DATABASE);
        if ($this->con->connect_errno) {
            $response = array();
            $response["success"] = 0;
            $response["message"] = "Error - Failed to connect to MySQL: " . $con->connect_error;
            exit(json_encode($response));
        }
        $this->user_override = null;
        $this->detect_user_role();
        return $this->con;
    }

    function close() {
        $this->con->close();
    }

    function detect_user_role() {
        if ($_POST['ROLESECRET'] == ROLE_ADMIN_SECRET) {
            $this->user_role = ROLE_ADMIN;
        } else if ($_COOKIE['ROLESECRET'] == ROLE_ADMIN_SECRET) {
            $this->user_role = ROLE_ADMIN;
        } else if (!empty($this->user_override)) {
            $this->user_role = ROLE_USER;
        } else if (isset($_POST['UPDATED_BY'])) {
            $this->user_role = ROLE_USER;
        } else {
            $this->user_role = ROLE_OTHER;
        }
    }

    function set_user($user) {
        $this->user_override = $user;
        $this->detect_user_role();
    }

    function can_read() {
        // Everybody can read
        return true;
    }

    function can_add() {
        // Admin and named users can add
        return $this->user_role == ROLE_ADMIN || $this->user_role == ROLE_USER;
    }

    function can_edit() {
        // Admin and named users can edit
        return $this->user_role == ROLE_ADMIN || $this->user_role == ROLE_USER;
    }

    function can_edit_completed() {
        // Modify completed items only for admins/same as delete users
        return $this->can_delete();
    }

    function can_delete() {
        // Only admin can delete
        return $this->user_role == ROLE_ADMIN;
    }
}

?>
