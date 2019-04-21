<?php

echo "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"";
echo "../index.css";
echo "\"></head><body>";

echo "<h2>User uploaded pictures</h2>";

$files = glob("user*");
for ($i = 0; $i < count($files); $i++) {
    $image = $files[$i];
    echo "<h3>".basename($image)." (".filesize($image).")</h3>";
    echo '<img width="50%" src="'.$image.'"/>';
}
echo "</body></html>";
?>
