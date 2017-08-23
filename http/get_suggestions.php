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

if (isset($_POST['LIMIT'])) {
    $limit = mysqli_real_escape_string($db, $_POST['LIMIT']);
} else {
    $limit = "100";
}

# Simple group not enough, we need correct date for order
$result = $db->query("SELECT l1.NAME, l1.INFO FROM pitems l1 LEFT OUTER JOIN pitems l2 ON (l1.NAME = l2.NAME AND l1.CREATION_DATE < l2.CREATION_DATE) WHERE l2.NAME is null AND l1.COMPLETION_DATE <> -1 ORDER BY l1.CREATION_DATE DESC LIMIT $limit");

$response["items"] = array();
while ($row = $result->fetch_assoc()) {
    $item = array();
    $item["NAME"] = $row["NAME"];
    if ($row["INFO"] !== null) {
        $item["INFO"] = $row["INFO"];
    }
    array_push($response["items"], $item);
}
$response["success"] = 1;

echo json_encode($response);
?>
