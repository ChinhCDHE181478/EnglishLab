const path = require("path");
const XLSX = require(path.join(__dirname, "../../frontend/node_modules/@e965/xlsx"));

const firstImage = [
  [2, "MEERN", "230104574"],
  [3, "35メ Hølløw", "230118652"],
  [4, "35メ ROBER 火", "230176861"],
  [5, "B H Matchetev", "230009463"],
  [6, "Crysta1", "230651863"],
  [7, "grakin", "230120694"],
  [8, "Daedom", "230341088"],
  [9, "35メ SMA", "231067543"],
  [10, "35メ Baby", "230005215"],
  [11, "乂 Ants Vlad", "230013738"],
  [12, "mw Diaboloiro68", "230040159"],
  [13, "Valhalla", "230723243"],
  [14, "35メ Ponka", "230090287"],
  [15, "35メ AlisterYin", "230120913"],
  [16, "m Phú lê", "230058683"],
  [17, "35メ Dany", "230702343"],
  [18, "35メ Syal", "230211754"],
  [19, "Hạng Ang", "230122949"],
  [20, "35メ Kerper", "230109411"],
  [21, "Marak", "230177612"],
  [22, "35メ Reskai", "230113140"],
  [23, "MrBeast98", "230008476"],
  [24, "35メ Kenpachi", "230139990"],
  [25, "35メ Lucifer", "230121503"],
  [26, "35メ tuthi", "230013600"],
  [27, "4huyền", "230010138"],
  [28, "35メ Craix G", "230005184"],
  [29, "35メ GARFIELD", "230017238"],
  [30, "35メ Luffy", "230017285"]
];

const secondImage = [
  [2, "M L Anser", "230174350"],
  [3, "Buzz Lightyear", "230769120"],
  [4, "A P babypigg", "230769857"],
  [5, "D L Yagamii", "230768978"],
  [6, "D L Kawaiiy", "230768967"],
  [7, "么 J F", "230277762"],
  [8, "Mellow", "230145748"],
  [9, "么 Đại Ái", "230296712"],
  [10, "Pips Sanzang", "230248557"],
  [11, "BIG whale", "230769103"],
  [12, "Secretary bird", "230770149"],
  [13, "么 Shiro", "230188055"],
  [14, "么Mistah Lakan", "230444878"],
  [15, "么MiX", "230153810"],
  [16, "◯AckermanLevi", "230323992"],
  [17, "VịtNè X Crying", "230222032"],
  [18, "R D jtachii37", "230124798"],
  [19, "H3AVEN", "230439926"],
  [20, "Demon R O K", "230123308"],
  [21, "么 Shira", "230142226"],
  [22, "Lyćan", "230194307"],
  [23, "么 KITAE C", "230228944"],
  [24, "msBadDemon", "230138248"],
  [25, "mwhoanght", "230125642"],
  [26, "Marko z Tropoi", "230842333"],
  [27, "dyanh", "230133555"],
  [28, "么 MIG", "230229084"]
];

function makeSheet(rows) {
  const worksheet = XLSX.utils.aoa_to_sheet([["STT", "Name", "ID"], ...rows]);
  worksheet["!cols"] = [{ wch: 8 }, { wch: 24 }, { wch: 14 }];
  worksheet["!autofilter"] = { ref: `A1:C${rows.length + 1}` };
  return worksheet;
}

const workbook = XLSX.utils.book_new();
XLSX.utils.book_append_sheet(workbook, makeSheet(firstImage), "Danh_sach_1");
XLSX.utils.book_append_sheet(workbook, makeSheet(secondImage), "Danh_sach_2");
XLSX.utils.book_append_sheet(workbook, makeSheet([
  ...firstImage.map((row) => [row[0], row[1], row[2]]),
  ...secondImage.map((row) => [row[0], row[1], row[2]])
]), "Tong_hop");

const output = path.join(__dirname, "EnglishLab-data-from-images.xlsx");
XLSX.writeFile(workbook, output, { bookSST: true, compression: true });
process.stdout.write(output);
