<?php

#ini_set('display_errors', 'On');

require_once __DIR__ . '/db_connect.php';

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (!$dbcon->can_read()) {
    $response["success"] = 0;
    $response["message"] = "Permission denied";
    exit(json_encode($response));
}


echo "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"";
echo "index.css";
echo "\"></head><body>";

echo "<h1>Stats</h1>";

$result = $db->query("SELECT COUNT(*) AS c FROM pitems WHERE COMPLETION_DATE = -1");
while ($row = $result->fetch_assoc()) {
    echo "Current: ".$row["c"]."<br/>";
}
$result = $db->query("SELECT COUNT(*) AS c FROM pitems WHERE COMPLETION_DATE > 0");
while ($row = $result->fetch_assoc()) {
    echo "Completed: ".$row["c"]."<br/>";
}

function stats_by($db, $column) {
    $result = $db->query("SELECT $column, COUNT($column) AS c FROM pitems GROUP BY $column ORDER BY c DESC");
    while ($row = $result->fetch_assoc()) {
        if ($row["c"] != 0) {
            echo htmlspecialchars($row[$column])."->".$row["c"]."<br/>";
        }
    }
}
echo "<h2>By name</h2>";
stats_by($db, "NAME");
echo "<h2>By creator</h2>";
stats_by($db, "CREATOR");
echo "<h2>By updater</h2>";
stats_by($db, "UPDATED_BY");

echo "</body></html>";

?>
