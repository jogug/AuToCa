{-# LANGUAGE DataKinds                   #-}

-- Copyright   : (c) 2013-2014 Brendan Hay <brendan.g.hay@gmail.com>
module Network.AWS.EC2.DeleteVpc
    (
    -- * Request
      DeleteVpc
    -- ** Request constructor
    , deleteVpc
    ) where

import Network.AWS.Prelude

-- | 'DeleteVpc' constructor.

deleteVpc :: Text -- ^ 'dv3VpcId'
          -> DeleteVpc
deleteVpc p1 = DeleteVpc
    { _dv3VpcId  = p1
    , _dv3DryRun = Nothing
    }

data DeleteVpcResponse = DeleteVpcResponse
    deriving (Eq, Ord, Read, Show, Generic)

-- | 'DeleteVpcResponse' constructor.
deleteVpcResponse :: DeleteVpcResponse

instance ToQuery DeleteVpc where
    toQuery DeleteVpc{..} = mconcat
        [ "DryRun" =? _dv3DryRun
        , "VpcId"  =? _dv3VpcId
        ]

instance AWSRequest DeleteVpc where
    type Sv DeleteVpc = EC2

    request  = post "DeleteVpc"

