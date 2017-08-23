<?php

$response = array();

require_once __DIR__ . '/db_connect.php';

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (!$dbcon->can_delete()) {
    $response["success"] = 0;
    $response["message"] = "Permission denied";
    exit(json_encode($response));
}

if (isset($_POST['ID'])) {
    $ID = mysqli_real_escape_string($db, $_POST['ID']);

    $result = $db->query("DELETE FROM pitems WHERE ID = $ID");

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
    $response["message"] = "Missing ID";
    echo json_encode($response);
}
?>
