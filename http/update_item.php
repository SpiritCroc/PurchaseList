<?php

#ini_set('display_errors', 'On');
#error_reporting(E_ALL);

$response = array();

require_once __DIR__ . '/db_connect.php';

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (!$dbcon->can_edit()) {
    $response["success"] = 0;
    $response["message"] = "Permission denied";
    exit(json_encode($response));
}

if (isset($_POST['ID']) && isset($_POST['NAME']) && (isset($_POST['UPDATED_BY']) || isset($_POST['CREATOR']))) {
    $ID = mysqli_real_escape_string($db, $_POST['ID']);
    $NAME = mysqli_real_escape_string($db, $_POST['NAME']);
    $INFO = mysqli_real_escape_string($db, $_POST['INFO']);
    if (isset($_POST['USAGE'])) {
        $USAGE = mysqli_real_escape_string($db, $_POST['USAGE']);
        $USAGE = ", USAGE1 = '$USAGE'";
    } else {
        # Don't overwrite usage when using legacy requests
        $USAGE = "";
    }
    if (isset($_POST['UPDATED_BY'])) {
        $UPDATED_BY = mysqli_real_escape_string($db, $_POST['UPDATED_BY']);
    } else {
        // Legacy
        $UPDATED_BY = mysqli_real_escape_string($db, $_POST['CREATOR']);
    }
    if (isset($_POST['CREATION_DATE'])) {
        $CREATION_DATE = mysqli_real_escape_string($db, $_POST['CREATION_DATE']);
    } else {
        $CREATION_DATE = number_format(round(microtime(true) * 1000), 0, '', '');
    }

    if ($dbcon->can_edit_completed()) {
        $COMPLETE_FILTER = "";
    } else {
        // User not allowed to edit completed entries
        $COMPLETE_FILTER = "AND COMPLETION_DATE = -1";
    }
    if (isset($_POST['PICTURE_URL'])) {
        $PICTURE_URL = mysqli_real_escape_string($db, $_POST['PICTURE_URL']);
        $PICTURE_URL = ", PICTURE_URL = '$PICTURE_URL'";
    } else {
        # Don't overwrite usage when using legacy requests
        $PICTURE_URL = "";
    }

    $result = $db->query("UPDATE pitems SET NAME = '$NAME', INFO = '$INFO', UPDATED_BY = '$UPDATED_BY', CREATION_DATE = '$CREATION_DATE'$USAGE$PICTURE_URL WHERE ID = $ID $COMPLETE_FILTER");

    if ($result) {
        $response["success"] = 1;
        $response["message"] = "Update successful";
        echo json_encode($response);
   } else {
        $response["success"] = 0;
        $response["message"] = mysqli_error($db);
        echo json_encode($response);
    }
} else {
    $response["success"] = 0;
    $response["message"] = "At least ID, NAME and UPDATED_BY (or CREATOR) need to be specified";
    echo json_encode($response);
}
?>
