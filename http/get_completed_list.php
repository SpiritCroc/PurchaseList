<?php

#ini_set('display_errors', 1);
#ini_set('display_startup_errors', 1);
#error_reporting(E_ALL);

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
    $sortorder = "ORDER BY COMPLETION_DATE DESC";
}

if (isset($_POST['GROUPING'])) {
    $grouping = mysqli_real_escape_string($db, $_POST['GROUPING']);
} else {
    $grouping = "";
}
if ($grouping == "hideOldDuplicates") {
    $request = "SELECT l1.* FROM pitems l1 LEFT OUTER JOIN pitems l2 ON (l1.NAME = l2.NAME AND l1.INFO = l2.INFO AND l1.CREATION_DATE < l2.CREATION_DATE) WHERE l2.ID is null AND";
} else {
    $request = "SELECT l1.* FROM pitems l1 WHERE";
}

$request .= " l1.COMPLETION_DATE != -1";

if (isset($_POST['SEARCH'])) {
    $search = mysqli_real_escape_string($db, $_POST['SEARCH']);
    $keywords = str_split($search);
    $filter ="l1.NAME LIKE '%".$search."%'";
    $filter .=" OR l1.INFO LIKE '%".$search."%'";
    $filter .=" OR l1.USAGE1 LIKE '%".$search."%'";
    $request .= " AND (".$filter.")";
}

$request .= " ".$sortorder;

$result = $db->query($request);

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
