const path = require("path");
const package = require(path.resolve(process.argv[2]));
package.scripts.install = undefined;
console.log(JSON.stringify(package, null, 2));
