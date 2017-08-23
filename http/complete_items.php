<?php

$response = array();

require_once __DIR__ . '/db_connect.php';

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (!$dbcon->can_edit()) {
    $response["success"] = 0;
    $response["message"] = "Permission denied";
    exit(json_encode($response));
}

if (isset($_POST['SELECTION'])) {
    $SELECTION = mysqli_real_escape_string($db, $_POST['SELECTION']);
    if (isset($_POST['COMPLETION_DATE'])) {
        $COMPLETION_DATE = mysqli_real_escape_string($db, $_POST['COMPLETION_DATE']);
    } else {
        $COMPLETION_DATE = round(microtime(true) * 1000);
    }
    if (isset($_POST['UPDATED_BY'])) {
        $UPDATED_BY = mysqli_real_escape_string($db, $_POST['UPDATED_BY']);
        $UPDATED_BY = ", UPDATED_BY = '$UPDATED_BY'";
    } else {
        $UPDATED_BY = "";
    }

    $result = $db->query("UPDATE pitems SET COMPLETION_DATE = '$COMPLETION_DATE'$UPDATED_BY WHERE $SELECTION");

    if ($result) {
        $response["success"] = 1;
        $response["message"] = "UPDATE successful";
        echo json_encode($response);
    } else {
        $response["success"] = 0;
        $response["message"] = mysqli_error($db);
        echo json_encode($response);
    }
} else {
    $response["success"] = 0;
    $response["message"] = "Missing SELECTION";
    echo json_encode($response);
}
?>
