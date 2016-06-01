local htmlparser = require("htmlparser")
local http = require('socket.http')
local app = require('waffle').CmdLine()

local search = function(req, res)
	local query = req.url.args.zoektermen
	local queryType = req.url.args.opZoeken
	local queryPage = req.url.args.page or 1
	local queryEan = ""
	if query == nil then
		res.send("Error")
	end
	-- Ean query
	if queryType == 1 then
		queryEan = query 
		query = ""
	end
	local body, c, l, h = http.request('http://livaad.nl/app/loaddata.php?artq='..query..'&eanq='..queryEan..'&producentq=&p='..queryPage)
	local root = htmlparser.parse(body)
	local products = root:select(".panel")

	local results = {}
	-- Get all products
	for i=1,#products do
		local header = products[i](".panel-heading")
		if #header >= 1 then
			local brand, name = unpack(header[1]:select('div'))
			brand = brand:getcontent()
			name, c = string.gsub(name:getcontent(), '<(.*)>', "")
			local rows = products[i]('.row')
			local attributes = {}
			for i=1,#rows do
				local k, v = unpack(rows[i]('div'))
				attributes[k:getcontent():gsub("[( |:)]", "")] = v:getcontent()
			end
			local bsm = 6 -- By default TODO
			local source = 1 -- By default TODO
			-- 1 is free, 2 is contains
			local _, starch = attributes["Tarwezetmeel"]:gsub("vrij", "vrij")
			starch = starch > 0 and 1 or 2
			local _, lactose = attributes["Lactose"]:gsub("vrij", "vrij")
			lactose = lactose > 0 and 1 or 2
			local datum = string.gsub(attributes["Checkdatum"], '<(.*)>(.*)</(.*)>', ""):gsub(" ", "")
			results[#results+1] = {name, attributes["EAN"], brand, attributes["Soort"], starch, lactose, bsm, datum, source}
		end
	end
    res.json({api_version=0.2, results=results})
end

app.get('/search.php', search)
app.get('/glutenvrij/search.php', search)
-- 127.0.0.1
app.listen()
