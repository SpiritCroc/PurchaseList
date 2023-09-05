<?php
#ini_set('display_errors', 'On');

try {
    require_once __DIR__ . '/db_connect.php';

    $dbcon = new DB_CONNECT();
    $db = $dbcon->con;

    if (!$dbcon->can_read()) {
        http_response_code(500);
        exit("Cannot read database");
    }
} catch (Exception $e) {
    #echo $e->getMessage(), "\n";
    http_response_code(500);
    exit("Exception while trying to access database");
}

$upload_max_filesize = ini_get('upload_max_filesize');
$post_max_size = ini_get('post_max_size');
$min_required_max_size = "50M";

# https://www.php.net/ini_get
function return_bytes($val) {
    $val = trim($val);
    $last = strtolower($val[strlen($val)-1]);
    switch($last) {
        // The 'G' modifier is available
        case 'g':
            $val *= 1024;
        case 'm':
            $val *= 1024;
        case 'k':
            $val *= 1024;
    }

    return $val;
}

echo "upload_max_filesize: " . $upload_max_filesize . "\n<br>";
echo "post_max_size: " . $post_max_size . "\n<br>";

if (return_bytes($upload_max_filesize) < return_bytes($min_required_max_size) ||
    return_bytes($post_max_size) < return_bytes($min_required_max_size)) {
    http_response_code(500);
    echo "Filesize config too small, need at least " . $min_required_max_size . "!\n";
    exit();
}
?>
