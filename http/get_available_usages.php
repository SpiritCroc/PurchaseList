<?php

$response = array();

require_once __DIR__ . '/db_connect.php';

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (!$dbcon->can_read()) {
    $response["success"] = 0;
    $response["message"] = "Permission denied";
    exit(json_encode($response));
}

# Simple group not enough, we need correct date for order
$result = $db->query("SELECT DISTINCT USAGE1 FROM pitems WHERE USAGE1 is not null AND USAGE1 <> ''  ORDER BY USAGE1 ASC");

$response["items"] = array();
while ($row = $result->fetch_assoc()) {
    $item = array();
    $item["USAGE"] = $row["USAGE1"];
    array_push($response["items"], $item);
}
$response["success"] = 1;

echo json_encode($response);
?>
