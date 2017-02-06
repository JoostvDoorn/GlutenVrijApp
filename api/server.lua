local htmlparser = require("htmlparser")
local http = require('socket.http')
local json = require('json')
local app = require('waffle').CmdLine()

local search = function(req, res)
  res.header('Access-Control-Allow-Origin', '*')
  local query = req.url.args.zoektermen
  local queryType = tonumber(req.url.args.opZoeken)
  local queryPage = tonumber(req.url.args.page) or 0
  local queryEan = ""
  if query == nil then
    res.send("Error")
  else
    -- Ean query
    if queryType == 1 then
      queryEan = query
      query = ""
    end
    print(query)
    local url = {'http://livaad.nl/app/loaddata.php', '?artq=', query, '&eanq=', queryEan, '&producentq=&p=', queryPage}
    if queryPage < 1 then
      url[1] = 'http://livaad.nl/database/'
    end
    local body, c, l, h = http.request(table.concat(url, ""))
    if body then
      local root = htmlparser.parse(body)
      if queryPage < 1 then
        local dataContainer = root:select("#data-container")
        if #dataContainer>=1 then
          root = htmlparser.parse(dataContainer[1]:getcontent())
        else
          res.send("Error")
        end
      end
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
          local bsm = "" -- By default TODO
          local source = "" -- By default TODO
          -- 1 is free, 2 is contains
          local _, starch = attributes["Tarwezetmeel"]:gsub("vrij", "vrij")
          starch = starch > 0 and 1 or 2
          local _, lactose = attributes["Lactose"]:gsub("vrij", "vrij")
          lactose = lactose > 0 and 1 or 2
          local datum = string.gsub(attributes["Checkdatum"], '<(.*)>(.*)</(.*)>', ""):gsub(" ", ""):gsub("-201", "") -- TODO: Fix in app
          datum = datum:sub(3,3) .. datum:sub(1,2) -- Last number year, month
          results[#results+1] = {name, attributes["EAN"], brand, attributes["Soort"], starch, lactose, bsm, datum, source}
        end
      end
      res.header('Content-Type', 'application/x-json')
      res.send(json.encode{api_version=0.2, results=results})
    else
      print("No body received")
      res.send("Error")
    end
  end
end

app.get('/search.php', search)
app.get('/glutenvrij/search.php', search)
app.get('/glutenvrij/search/', search)
app.options('/glutenvrij/search/', search)
-- 127.0.0.1
app.listen()
