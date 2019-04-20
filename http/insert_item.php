<?php

#ini_set('display_errors', 'On');
#error_reporting(E_ALL);

$response = array();

require_once __DIR__ . '/db_connect.php';

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (!$dbcon->can_add()) {
    $response["success"] = 0;
    $response["message"] = "Permission denied";
    exit(json_encode($response));
}

if (isset($_POST['NAME']) && isset($_POST['CREATOR'])) {
    if (isset($_POST['ID'])) {
        $ID = mysqli_real_escape_string($db, $_POST['ID']);
    } else {
        $ID = number_format(round(microtime(true) * 1000), 0, '', '');
    }
    $NAME = mysqli_real_escape_string($db, $_POST['NAME']);
    $INFO = mysqli_real_escape_string($db, $_POST['INFO']);
    if (isset($_POST['USAGE'])) {
        $USAGE = mysqli_real_escape_string($db, $_POST['USAGE']);
    } else {
        $USAGE = "";
    }
    $CREATOR = mysqli_real_escape_string($db, $_POST['CREATOR']);
    if (isset($_POST['CREATION_DATE'])) {
        $CREATION_DATE = mysqli_real_escape_string($db, $_POST['CREATION_DATE']);
    } else {
        $CREATION_DATE = number_format(round(microtime(true) * 1000), 0, '', '');
    }
    $COMPLETION_DATE = -1;
    if (isset($_POST['PICTURE_URL'])) {
        $PICTURE_URL = "'".mysqli_real_escape_string($db, $_POST['PICTURE_URL'])."'";
    } else {
        $PICTURE_URL = "NULL";
    }

    $result = $db->query("INSERT INTO pitems(ID, NAME, INFO, USAGE1, CREATOR, CREATION_DATE, COMPLETION_DATE, PICTURE_URL) VALUES ('$ID', '$NAME', '$INFO', '$USAGE', '$CREATOR', '$CREATION_DATE', '$COMPLETION_DATE', $PICTURE_URL)");

    if ($result) {
        $response["success"] = 1;
        $response["message"] = "Insert successful";
        echo json_encode($response);
   } else {
        $response["success"] = 0;
        $response["message"] = mysqli_error($db);
        echo json_encode($response);
    }
} else {
    $response["success"] = 0;
    $response["message"] = "At least NAME and CREATOR need to be specified";
    echo json_encode($response);
}
?>
