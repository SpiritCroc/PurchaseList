<?php

#ini_set('display_errors', 'On');

require_once __DIR__ . '/../db_connect.php';

function is_delete_allowed($filename) {
    # Sanity checks
    $mydir = realpath(__DIR__);
    $file = pathinfo(realpath($filename));
    if ($file['dirname']  != $mydir || strpos($file['basename'], 'user') !== 0) {
        // Only deleting user_ from this directory allowed
        return false;
    }
    $dbcon = new DB_CONNECT();
    $db = $dbcon->con;
    if ($dbcon->can_delete()) {
        $result = $db->query("SELECT ID FROM pitems WHERE PICTURE_URL LIKE \"%".mysqli_real_escape_string($db, $filename)."\"");
        return $result->num_rows == 0;
    } else {
        return false;
    }
}


if (isset($_POST["DEL_FILE"])) {
    $image = htmlspecialchars($_POST["DEL_FILE"]);
    if (is_delete_allowed($image)) {
        unlink($image);
    }
    # Refresh site
    header("Location: ".$_SERVER['REQUEST_URI']);
    exit();
}
echo "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"";
echo "../index.css";
echo "\"></head><body>";

echo "<h2>User uploaded pictures</h2>";

$files = glob("user*");
for ($i = 0; $i < count($files); $i++) {
    $image = $files[$i];
    echo "<h3>".basename($image)." (".filesize($image).")</h3>";
    echo '<img width="100" src="'.$image.'"/>';

    if (is_delete_allowed($image)) {
        echo "<form method=\"post\">";
        echo "<input type=\"hidden\" name=\"DEL_FILE\" value=\"".htmlspecialchars($image)."\"/>";
        echo "<input type=\"submit\" value=\"L&ouml;schen\"/>";
        echo "</form>";
    }
}
echo "</body></html>";
?>
