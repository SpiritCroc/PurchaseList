<?php

#ini_set('display_errors', 'On');

require_once __DIR__ . '/db_connect.php';

$sortorder = "ORDER BY CREATION_DATE DESC";
$completedsortorder = "ORDER BY COMPLETION_DATE DESC";

$dbcon = new DB_CONNECT();
$db = $dbcon->con;

if (isset($_POST["CREATOR"])) {
    $creator = mysqli_real_escape_string($db, $_POST["CREATOR"]);
    setcookie("CREATOR", $creator);
} elseif (isset($_COOKIE["CREATOR"])) {
    $creator = mysqli_real_escape_string($db, $_COOKIE["CREATOR"]);
} else {
    $creator = null;
}

$dbcon->set_user($creator);

if (isset($_POST["SITE_SHOW_COMPLETED"])) {
    if ($_POST["SITE_SHOW_COMPLETED"] == 'TRUE') {
        $showcompleted = TRUE;
    } else {
        $showcompleted = FALSE;
    }
    setcookie("SHOW_COMPLETED", $showcompleted);
} else {
    $showcompleted = isset($_COOKIE["SHOW_COMPLETED"]) && $_COOKIE["SHOW_COMPLETED"];
}

$edititem = FALSE;


echo "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"";
echo "index.css";
echo "\"></head><body>";


if (!empty($creator)) {

    if (isset($_POST["NAME"]) && $_POST["NAME"] != '') {
        if (isset($_POST["ID"])) {
            # Edit entry
            if (!$dbcon->can_edit()) {
                $response["success"] = 0;
                $response["message"] = "Permission denied";
                exit(json_encode($response));
            }
            $id = mysqli_real_escape_string($db, $_POST["ID"]);
            $name = mysqli_real_escape_string($db, $_POST["NAME"]);
            $info = mysqli_real_escape_string($db, $_POST["INFO"]);
            $usage = mysqli_real_escape_string($db, $_POST["USAGE"]);
            $creation_date = number_format(round(microtime(true) * 1000), 0, '', '');
            $picture_url = mysqli_real_escape_string($db, $_POST["PICTURE_URL"]);

            $result = $db->query("UPDATE pitems SET NAME = '$name', INFO = '$info', USAGE1 = '$usage', CREATION_DATE = '$creation_date', UPDATED_BY = '$creator', PICTURE_URL = '$picture_url' WHERE ID = $id");
        } else {
            # Add entry
            if (!$dbcon->can_add()) {
                $response["success"] = 0;
                $response["message"] = "Permission denied";
                exit(json_encode($response));
            }
            $id = number_format(round(microtime(true) * 1000), 0, '', '');
            $name = mysqli_real_escape_string($db, $_POST["NAME"]);
            $info = mysqli_real_escape_string($db, $_POST["INFO"]);
            $usage = mysqli_real_escape_string($db, $_POST["USAGE"]);
            $creation_date = number_format(round(microtime(true) * 1000), 0, '', '');
            $completion_date = -1;
            $picture_url = mysqli_real_escape_string($db, $_POST["PICTURE_URL"]);

            $result = $db->query("INSERT INTO pitems(ID, NAME, INFO, USAGE1, CREATOR, CREATION_DATE, COMPLETION_DATE, PICTURE_URL) VALUES ('$id', '$name', '$info', '$usage', '$creator', '$creation_date', '$completion_date', '$picture_url')");
        }
        # Refresh site
        header("Location: ".$_SERVER['REQUEST_URI']);
        exit();
    } elseif (isset($_POST["ID"])) {
        if (isset($_POST["COMPLETION_DATE"])) {
            if (!$dbcon->can_edit()) {
                $response["success"] = 0;
                $response["message"] = "Permission denied";
                exit(json_encode($response));
            }
            $id = mysqli_real_escape_string($db, $_POST["ID"]);
            $completion_date = number_format(round(microtime(true) * 1000), 0, '', '');

            $result = $db->query("UPDATE pitems SET COMPLETION_DATE = '$completion_date', UPDATED_BY = '$creator' WHERE ID = $id");
            # Refresh site
            header("Location: ".$_SERVER['REQUEST_URI']);
            exit();
        } else {
            $edititem = TRUE;
        }
    }

    if (!$dbcon->can_read()) {
        $response["success"] = 0;
        $response["message"] = "Permission denied";
        exit(json_encode($response));
    }

    echo "<div align=\"right\">";
    echo "<form method=\"post\">";
    echo "Angemeldet als $creator. ";
    echo "<input type=\"hidden\" name=\"CREATOR\" value=\"\"/>";
    echo "<input type=\"submit\" value=\"Abmelden\"/>";
    echo "</form>";
    echo "</div>";
    echo "<h1>Einkaufsliste</h1>";

    $result = $db->query("SELECT * FROM pitems WHERE COMPLETION_DATE = -1 $sortorder");

    echo "<table><tr><th>Produkt</th><th>Info</th><th>Verwendung</th><th>Datum</th><th>Ersteller</th><th>Bearbeitet von</th></tr>";
    while ($row = $result->fetch_assoc()) {
        $item = array();
        $item["ID"] = htmlspecialchars($row["ID"]);
        $item["NAME"] = htmlspecialchars($row["NAME"]);
        if ($row["INFO"] !== null) {
            $item["INFO"] = htmlspecialchars($row["INFO"]);
        }
        if ($row["USAGE1"] !== null) {
            $item["USAGE"] = htmlspecialchars($row["USAGE1"]);
        }
        $item["CREATOR"] = htmlspecialchars($row["CREATOR"]);
        if ($row["UPDATED_BY"] !== null) {
            $item["UPDATED_BY"] = htmlspecialchars($row["UPDATED_BY"]);
        }
        $item["CREATION_DATE"] = htmlspecialchars($row["CREATION_DATE"]);
        if ($row["COMPLETION_DATE"] !== null) {
            $item["COMPLETION_DATE"] = htmlspecialchars($row["COMPLETION_DATE"]);
        }
        if ($row["PICTURE_URL"] !== null) {
            $item["PICTURE_URL"] = htmlspecialchars($row["PICTURE_URL"]);
        }

        $itemdate = date("d.m.Y", ($item["CREATION_DATE"]/1000));
        $name = $item["NAME"];
        if (!empty($item["PICTURE_URL"])) {
            $name = "<a target=\"_blank\" href=\"".$item["PICTURE_URL"]."\">$name</a>";
        }
        echo "<tr><td>".$name."</td><td>".$item["INFO"]."</td><td>".$item["USAGE"]."</td><td>".$itemdate."</td><td>".$item["CREATOR"]."</td><td>".$item["UPDATED_BY"]."</td>";
        echo "<td><div align=\"center\"><form method=\"post\" action=\"#edit\">";
        echo "<input type=\"hidden\" name=\"ID\" value=\"".$item["ID"]."\"/>";
        echo "<input type=\"hidden\" name=\"CREATOR\" value=\"".$creator."\"/>";
        echo "<input type=\"submit\" value=\"Bearbeiten\"/>";
        echo "</form>";
        echo "</div></td><td><div align=\"center\">";
        echo "<form method=\"post\">";
        echo "<input type=\"hidden\" name=\"ID\" value=\"".$item["ID"]."\"/>";
        echo "<input type=\"hidden\" name=\"CREATOR\" value=\"".$creator."\"/>";
        echo "<input type=\"hidden\" name=\"COMPLETION_DATE\" value=\"\"/>";
        echo "<input type=\"submit\" value=\"Erledigt\"/>";
        echo "</form>";
        echo "</div></td>";
        if (isset($_POST["BOSS"]) && $_POST["BOSS"] == "true") {
            echo "<td>".$item["ID"]."</td>";
        }
        echo "</tr>";

        if ($item["ID"] == $_POST["ID"]) {
            $editname = $item["NAME"];
            $editinfo = $item["INFO"];
            $editusage = $item["USAGE"];
            $picture_url = $item["PICTURE_URL"];
        }
    }

    echo "</table>";

    if ($edititem) {
        echo "<a name=\"edit\">";
        echo "<h2>Eintrag bearbeiten</h2>";
        echo "</a>";
        echo "<form  method=\"post\">";
        echo "Produkt:<br/><input type=\"text\" name=\"NAME\" value=\"".$editname."\"/><br/>";
        echo "Info:<br/><input type=\"text\" name=\"INFO\" value=\"".$editinfo."\"/><br/>";
        echo "Verwendung:<br/><input type=\"text\" name=\"USAGE\" value=\"".$editusage."\"/><br/>";
        echo "Bildadresse:<br/><input type=\"text\" name=\"PICTURE_URL\" value=\"".$picture_url."\"/><br/>";
        echo "<input type=\"hidden\" name=\"ID\" value=\"".mysqli_real_escape_string($db, $_POST["ID"])."\"/>";
        echo "<input type=\"hidden\" name=\"SITE_ACTION\" value=\"edit\"/>";
        echo "<input type=\"submit\" value=\"Aktualisieren\"/>";
        echo "</form>";
        echo "<form method=\"post\">";
        echo "<input type=\"submit\" value=\"Bearbeiten abbrechen\"/>";
        echo "</form>";

        echo "<form target=\"_blank\" action=\"picture_add.php\" method=\"post\" enctype=\"multipart/form-data\">";
        echo "<label for=\"picture_input\" class=\"btn\">Eigenes Bild: </label>";
        echo "<input id=\"picture_input\" type=\"file\" name=\"PICTURE\" />";
        echo "<input type=\"hidden\" name=\"ID\" value=\"".mysqli_real_escape_string($db, $_POST["ID"])."\"/>";
        echo "<input type=\"hidden\" name=\"UPDATED_BY\" value=\"".$creator."\"/>";
        echo "<input type=\"submit\" value=\"Hochladen\" />";
        echo "</p>";
        echo "</form>";
    } else {
        echo "<h2>Eintrag hinzuf&uuml;gen</h2>";
        echo "<form  method=\"post\">";
        echo "Produkt:<br/><input type=\"text\" name=\"NAME\"/><br/>";
        echo "Info:<br/><input type=\"text\" name=\"INFO\"/><br/>";
        echo "Verwendung:<br/><input type=\"text\" name=\"USAGE\"/><br/>";
        echo "Bildadresse:<br/><input type=\"text\" name=\"PICTURE_URL\"/><br/>";
        echo "<input type=\"hidden\" name=\"SITE_ACTION\" value=\"add\"/>";
        echo "<input type=\"submit\" value=\"Hinzuf&uuml;gen\"/>";
        echo "</form>";
    }

    echo "<a name=\"completed\">";
    echo "<h1>Erledigte</h1>";
    echo "</a>";
    if ($showcompleted) {
        echo "<form method=\"post\">";
        echo "<input type=\"hidden\" name=\"SITE_SHOW_COMPLETED\" value=\"FALSE\"/>";
        echo "<input type=\"submit\" value=\"Erledigte verstecken\"/>";
        echo "</form>";
        $result = $db->query("SELECT * FROM pitems WHERE COMPLETION_DATE != -1 $completedsortorder");
        echo "<table><tr><th>Produkt</th><th>Info</th><th>Verwendung</th><th>Datum</th><th>Ersteller</th><th>Erledigt von</th></tr>";
        while ($row = $result->fetch_assoc()) {
            $item = array();
            $item["ID"] = htmlspecialchars($row["ID"]);
            $item["NAME"] = htmlspecialchars($row["NAME"]);
            if ($row["INFO"] !== null) {
                $item["INFO"] = htmlspecialchars($row["INFO"]);
            }
            if ($row["USAGE1"] !== null) {
                $item["USAGE"] = htmlspecialchars($row["USAGE1"]);
            }
            $item["CREATOR"] = htmlspecialchars($row["CREATOR"]);
            if ($row["UPDATED_BY"] !== null) {
                $item["UPDATED_BY"] = htmlspecialchars($row["UPDATED_BY"]);
            }
            $item["CREATION_DATE"] = htmlspecialchars($row["CREATION_DATE"]);
            if ($row["COMPLETION_DATE"] !== null) {
                $item["COMPLETION_DATE"] = htmlspecialchars($row["COMPLETION_DATE"]);
            }
            if ($row["PICTURE_URL"] !== null) {
                $item["PICTURE_URL"] = htmlspecialchars($row["PICTURE_URL"]);
            }

            $itemdate=date("d.m.Y", ($item["COMPLETION_DATE"]/1000));

            $name = $item["NAME"];
            if (!empty($item["PICTURE_URL"])) {
                $name = "<a target=\"_blank\" href=\"".$item["PICTURE_URL"]."\">$name</a>";
            }

            echo "<tr><td>".$name."</td><td>".$item["INFO"]."</td><td>".$item["USAGE"]."</td><td>".$itemdate."</td><td>".$item["CREATOR"]."</td><td>".$item["UPDATED_BY"]."</td>";
            if (isset($_POST["BOSS"]) && $_POST["BOSS"] == "true") {
                echo "<td>".$item["ID"]."</td>";
            }
            echo "</tr>";
        }
        echo "</table>";
    } else {
        echo "<form method=\"post\" action=\"#completed\">";
        echo "<input type=\"hidden\" name=\"SITE_SHOW_COMPLETED\" value=\"TRUE\"/>";
        echo "<input type=\"submit\" value=\"Erledigte anzeigen\"/>";
        echo "</form>";
    }

    echo "<div align=\"right\"><form method=\"post\">";
    if (isset($_POST["BOSS"]) && $_POST["BOSS"] == "true") {
        echo "<input type=\"hidden\" name=\"BOSS\" value=\"false\"/>";
        echo "<input type=\"submit\" value=\"Ich bin nicht der Boss\"/>";
    } else {
        echo "<input type=\"hidden\" name=\"BOSS\" value=\"true\"/>";
        echo "<input type=\"submit\" value=\"Ich bin der Boss\"/>";
    }
    echo "</form></div>";

} else {
    echo "<h1>Wer?</h1>";
    echo "<form method=\"post\">";
    echo "Name: <input type=\"text\" name=\"CREATOR\"/>";
    echo "<input type=\"submit\" value=\"Weiter\"/>";
    echo "</form>";
}


echo "</body></html>";

?>
