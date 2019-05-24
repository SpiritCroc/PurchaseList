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

if (isset($_POST['ID']) && isset($_FILES['PICTURE']) && isset($_POST['UPDATED_BY'])) {
    $ID = mysqli_real_escape_string($db, $_POST['ID']);
    $UPDATED_BY = mysqli_real_escape_string($db, urldecode($_POST['UPDATED_BY']));
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

    $PICTURE_ID = number_format(round(microtime(true) * 1000), 0, '', '');
    $PICTURE_ADDR = "picture_uploads/user".$PICTURE_ID.".".pathinfo($_FILES['PICTURE']['name'], PATHINFO_EXTENSION);
    $PICTURE_URL = "";

    if (move_uploaded_file($_FILES['PICTURE']['tmp_name'], $PICTURE_ADDR)) {
        $PICTURE_URL = $PICTURE_ADDR;
    } else {
        $response["success"] = 0;
        $response["message"] = "File upload failed for ".$PICTURE_ADDR;
        exit(json_encode($response));
    }

    $result = $db->query("UPDATE pitems SET UPDATED_BY = '$UPDATED_BY', CREATION_DATE = '$CREATION_DATE', PICTURE_URL = '$PICTURE_URL' WHERE ID = $ID $COMPLETE_FILTER");

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
    $response["message"] = "At least ID, UPDATED_BY and PICTURE required";
    echo json_encode($response);
}
?>
