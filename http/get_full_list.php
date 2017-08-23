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

if (isset($_POST['SORTORDER'])) {
    $sortorder = mysqli_real_escape_string($db, $_POST['SORTORDER']);
} else {
    $sortorder = "ORDER BY CREATION_DATE DESC";
}

$result = $db->query("SELECT * FROM pitems $sortorder");

$response["items"] = array();
while ($row = $result->fetch_assoc()) {
    $item = array();
    $item["ID"] = $row["ID"];
    $item["NAME"] = $row["NAME"];
    if ($row["INFO"] !== null) {
        $item["INFO"] = $row["INFO"];
    }
    $item["CREATOR"] = $row["CREATOR"];
    if ($row["UPDATED_BY"] !== null) {
        $item["UPDATED_BY"] = $row["UPDATED_BY"];
    }
    $item["CREATION_DATE"] = $row["CREATION_DATE"];
    if ($row["COMPLETION_DATE"] !== null) {
        $item["COMPLETION_DATE"] = $row["COMPLETION_DATE"];
    }
    if ($row["USAGE1"] !== null) {
        $item["USAGE"] = $row["USAGE1"];
    }
    array_push($response["items"], $item);
}
$response["success"] = 1;

echo json_encode($response);
?>
