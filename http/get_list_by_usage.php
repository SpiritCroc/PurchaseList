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

$sortorder = "ORDER BY USAGE1 ASC, NAME ASC";

# Filter out duplicates (use latest one if multiple found), then ensure non-empty usage
$result = $db->query("SELECT l1.* FROM pitems l1 LEFT OUTER JOIN pitems l2 ON (l1.NAME = l2.NAME AND l1.INFO = l2.INFO AND l1.USAGE1 = l2.USAGE1 AND l1.CREATION_DATE < l2.CREATION_DATE) WHERE l2.ID is null AND l1.USAGE1 IS NOT NULL AND l1.USAGE1 <> '' $sortorder");

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
    if ($row["PICTURE_URL"] !== null) {
        $item["PICTURE_URL"] = $row["PICTURE_URL"];
    }
    array_push($response["items"], $item);
}
$response["success"] = 1;

echo json_encode($response);
?>
